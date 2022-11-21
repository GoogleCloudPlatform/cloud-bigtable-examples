/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.bigtable.scaler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.bigtable.admin.v2.Cluster;
import com.google.cloud.bigtable.grpc.BigtableClusterName;
import com.google.cloud.bigtable.grpc.BigtableClusterUtilities;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.cloud.monitoring.v3.PagedResponseWrappers.ListTimeSeriesPagedResponse;
import com.google.monitoring.v3.ListTimeSeriesRequest.TimeSeriesView;
import com.google.monitoring.v3.Point;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.Timestamp;

/**
 * An example that leverages <a href="https://cloud.google.com/bigtable/docs/">Cloud Bigtable</a>
 * metrics from the <a href="https://cloud.google.com/monitoring/api/v3/">Stackdriver Monitoring
 * API</a> in order to resize a Cloud Bigtable cluster. This example uses the server's CPU load
 * which is an indicator of overall usage of a cluster. If a cluster is overloaded, performance
 * drops. This example looks at the CPU load metric, which is the key indicator that a cluster is
 * overloaded.  This example is not a comprehensive solution, but rather a starting point by which
 * a product grade auto-scaler can be built that is targted to your specific situation.
 */
public class MetricScaler {

  /**
   * This metric represents the average CPU load of a cluster. More metrics can be found
   * <a href="https://cloud.google.com/monitoring/api/metrics#gcp-bigtable">here</a>. Other metrics
   * such as "server/latencies" and "server/sent_bytes_count" can also be useful
   */
  public static final String CPU_METRIC = "bigtable.googleapis.com/cluster/cpu_load";

  /**
   * The minimum number of nodes to use. The default minimum is 3. If you have a lot of data, the
   * rule of thumb is to not go below 2.5 TB per node for SSD clusters, and 8 TB for HDD.
   * The bigtable.googleapis.com/disk/bytes_used metric is useful in figuring out the minimum number
   * of nodes.
   */
  public static final int MIN_NODE_COUNT = 3;

  /**
   * The maximum number of nodes to use. The default maximum is 30 nodes per zone. If you need more
   * quota, you can request more by following the instructions
   * <a href="https://cloud.google.com/bigtable/quota">here</a>.
   */
  public static final int MAX_NODE_COUNT = 30;

  /**
   * The number of minutes to wait between checks for scaling operations. It takes a few minutes for
   * changes to the cluster size to have an impact.
   */
  public static final long MINUTES_BETWEEN_CHECKS = 10;

  /**
   * The number of nodes to increase or decrease the cluster by.
   */
  public static final int SIZE_CHANGE_STEP = 3;

  /**
   * This is the percentage of average CPU used at which to reduce the number of nodes.
   * <p>
   * Reducing the number of nodes at 50% average CPU utilization will work for cases where high
   * throughput is the main concern of the system. Systems that are latency sensitive, such as
   * online applications, should aim for a lower percentage.
   */
  public static double CPU_PERCENT_TO_DOWNSCALE = .5;

  /**
   * This is the percentage of average CPU used at which to increase the number of nodes.
   * <p>
   * Increase the number of nodes at 70% average CPU utilization is a pretty safe number as a
   * balance of throughput and latency. This number can go as high as 80% for batch systems that
   * don't care too much about latency. If this number is too high, latency will have a sudden rapid
   * increase.
   */
  public static double CPU_PERCENT_TO_UPSCALE = .7;

  private static Timestamp timeXMinutesAgo(int minutesAgo) {
    int secondsAgo = minutesAgo * 60;
    long timeInSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
    return Timestamp.newBuilder().setSeconds(timeInSeconds - secondsAgo).build();
  }

  private ProjectName projectName;
  private String clusterId;
  private String zoneId;

  /**
   * This object can get the size of a cluster and resize it.
   */
  private BigtableClusterUtilities clusterUtility;

  /**
   * This object can read metrics from stackdriver. See more <a href=
   * "https://cloud.google.com/monitoring/docs/reference/libraries#client-libraries-install-java">here<a>.
   */
  private MetricServiceClient metricServiceClient;

  /**
   * Constructor for the auto-scaler with the minimum required information: project and instance ids.
   * @param projectId
   * @param instanceId
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public MetricScaler(String projectId, String instanceId)
      throws GeneralSecurityException, IOException {
    clusterUtility = BigtableClusterUtilities.forInstance(projectId, instanceId);
    Cluster cluster = clusterUtility.getSingleCluster();
    this.clusterId = new BigtableClusterName(cluster.getName()).getClusterId();
    this.zoneId = BigtableClusterUtilities.getZoneId(cluster);
    // Instantiates a client
    metricServiceClient = MetricServiceClient.create();
    projectName = ProjectName.create(projectId);
  }

  /**
   * Retrieves the latest value for the metric defined by {@link #CPU_METRIC}.
   * @return
   * @throws IOException
   */
  Point getLatestValue() throws IOException {
    // [START bigtable_cpu]
    Timestamp now = timeXMinutesAgo(0);
    Timestamp fiveMinutesAgo = timeXMinutesAgo(5);
    TimeInterval interval =
        TimeInterval.newBuilder().setStartTime(fiveMinutesAgo).setEndTime(now).build();
    String filter = "metric.type=\"" + CPU_METRIC + "\"";
    ListTimeSeriesPagedResponse response =
        metricServiceClient.listTimeSeries(projectName, filter, interval, TimeSeriesView.FULL);
    return response.getPage().getValues().iterator().next().getPointsList().get(0);
    // [END bigtable_cpu]
  }

  /**
   * Creates a {@link Runnable} that will increase the number of nodes by 3 if the CPU is over 70%,
   * and reduce the count by 3 if the CPU is below 50%.
   */
  public Runnable getRunnable() {
    return new Runnable() {
      @Override
      public void run() {
        try {
          // [START bigtable_scale]
          double latestValue = getLatestValue().getValue().getDoubleValue();
          if (latestValue < CPU_PERCENT_TO_DOWNSCALE) {
            int clusterSize = clusterUtility.getClusterNodeCount(clusterId, zoneId);
            if (clusterSize > MIN_NODE_COUNT) {
              clusterUtility.setClusterSize(clusterId, zoneId,
                Math.max(clusterSize - SIZE_CHANGE_STEP, MIN_NODE_COUNT));
            }
          } else if (latestValue > CPU_PERCENT_TO_UPSCALE) {
            int clusterSize = clusterUtility.getClusterNodeCount(clusterId, zoneId);
            if (clusterSize <= MAX_NODE_COUNT) {
              clusterUtility.setClusterSize(clusterId, zoneId,
                Math.min(clusterSize + SIZE_CHANGE_STEP, MAX_NODE_COUNT));
            }
          }
          // [END bigtable_scale]
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  /**
   * Given a project id and instance id, checks a the CPU usage every minute, and adjust the node
   * count accordingly.
   * @param args an Array containing projectId and instanceId to scale.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void main(String[] args) throws IOException, GeneralSecurityException {
    if (args.length < 2) {
      System.out.println("Usage: " + MetricScaler.class.getName() + " <project-id> <instance-id>");
      System.exit(-1);
    }
    MetricScaler scaler = new MetricScaler(args[0], args[1]);
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(scaler.getRunnable(), 0, MINUTES_BETWEEN_CHECKS, TimeUnit.MINUTES);
  }
}
