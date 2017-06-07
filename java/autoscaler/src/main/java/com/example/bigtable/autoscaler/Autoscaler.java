
/**
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
package com.example.bigtable.autoscaler;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.monitoring.v3.MonitoringScopes;
import com.google.api.services.monitoring.v3.model.Point;
import com.google.bigtable.admin.v2.Cluster;
import com.google.cloud.bigtable.grpc.BigtableClusterName;
import com.google.cloud.bigtable.grpc.BigtableClusterUtilities;

/**
 * An example that leverages <a href="https://cloud.google.com/bigtable/docs/">Cloud Bigtable</a>
 * metrics from the <a href="https://cloud.google.com/monitoring/api/v3/">Stackdriver Monitoring
 * API</a> in order to resize a Cloud Bigtable cluster. This example uses the server's CPU load
 * which is an indicator of overall usage of a cluster. If a cluster is overloaded, performance
 * drops. This example looks at the CPU load metric, which is the key indicator that a cluster is
 * overloaded.  This example is not a comprehensive solution, but rather a starting point by which
 * a product grade auto-scaler can be built that is targted to your specific situation.
 */
public class Autoscaler {

  /**
   * This metric represents the average CPU load of a cluster. More metrics can be found
   * <a href="https://cloud.google.com/monitoring/api/metrics#gcp-bigtable">here</a>. Other metrics
   * such as "server/latencies" and "server/sent_bytes_count" can also be useful
   */
  public static final String CPU_METRIC = "bigtable.googleapis.com/cluster/cpu_load";

  /**
   * The minimum number of nodes to use. The default minimum is 3. If you have a lot of data, the
   * rule of thumb is to not go below 1 node per terabyte for SSD clusters, and 5 terabytes for HDD.
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
   * Builds and returns a CloudMonitoring service object authorized with the
   * application default credentials.
   *
   * @return CloudMonitoring service object that is ready to make requests.
   * @throws GeneralSecurityException if authentication fails.
   * @throws IOException              if authentication fails.
   */
  static Monitoring createMonitoringService() throws GeneralSecurityException, IOException {
    // Grab the Application Default Credentials from the environment.
    GoogleCredential credential = GoogleCredential.getApplicationDefault()
        .createScoped(MonitoringScopes.all());

    // Create and return the CloudMonitoring service object
    return new Monitoring.Builder(
        new NetHttpTransport(),
        new JacksonFactory(),
        credential)
        .setApplicationName("Cloud Bigtable Autoscaler")
        .build();
  }

  public static SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

  static {
    rfc3339.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  protected static String format(DateTime dt) {
    return rfc3339.format(dt.toDate());
  }

  private String projectId;
  private String clusterId;
  private String zoneId;
  private Monitoring monitoringService;
  private BigtableClusterUtilities clusterUtility;

  /**
   * Constructor for the auto-scaler with the minimum required information: project and instance ids.
   * @param projectId
   * @param instanceId
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public Autoscaler(String projectId, String instanceId)
      throws GeneralSecurityException, IOException {
    this.projectId = projectId;
    clusterUtility = BigtableClusterUtilities.forInstance(projectId, instanceId);
    Cluster cluster = clusterUtility.getSingleCluster();
    this.clusterId = new BigtableClusterName(cluster.getName()).getClusterId();
    this.zoneId = BigtableClusterUtilities.getZoneId(cluster);
    this.monitoringService = createMonitoringService();
  }

  /**
   * Constructor with project, instance, cluster and zone ids.
   * @param projectId
   * @param instanceId
   * @param clusterId
   * @param zoneId
   * @throws GeneralSecurityException
   * @throws IOException
   */
  public Autoscaler(String projectId, String instanceId, String clusterId, String zoneId)
      throws GeneralSecurityException, IOException {
    this.projectId = projectId;
    this.clusterId = clusterId;
    this.zoneId = zoneId;
    this.monitoringService = createMonitoringService();
    clusterUtility = BigtableClusterUtilities.forInstance(projectId, instanceId);
  }

  /**
   * Retrieves the latest value for the metric defined by {@link #CPU_METRIC}.
   * @return
   * @throws IOException
   */
  Point getLatestValue() throws IOException {
    return this.monitoringService.projects().timeSeries()
        .list("projects/" + this.projectId)
        .setFilter("metric.type=\"" + CPU_METRIC + "\"")
        .setPageSize(1)
        .setIntervalStartTime(format(new DateTime().minusMinutes(5)))
        .setIntervalEndTime(format(new DateTime()))
        .execute().getTimeSeries().get(0).getPoints().get(0);
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
          double latestValue = getLatestValue().getValue().getDoubleValue();
          System.out.println(latestValue);
          if (latestValue < .5) {
            int clusterSize = clusterUtility.getClusterNodeCount(clusterId, zoneId);
            if (clusterSize > MIN_NODE_COUNT) {
              clusterUtility.setClusterSize(clusterId, zoneId,
                Math.max(clusterSize - 3, MIN_NODE_COUNT));
            }
          } else if (latestValue > .7) {
            int clusterSize = clusterUtility.getClusterNodeCount(clusterId, zoneId);
            if (clusterSize <= MAX_NODE_COUNT) {
              clusterUtility.setClusterSize(clusterId, zoneId,
                Math.min(clusterSize + 3, MAX_NODE_COUNT));
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };
  }

  /**
   * Given a project id and instance id, checks a the CPU usage every minute, and adjust the node
   * count accordingly.
   * @param args an Array containing projectId and instanceId to autoscale.
   * @throws IOException
   * @throws GeneralSecurityException
   */
  public static void main(String[] args) throws IOException, GeneralSecurityException {
    Autoscaler autoscaler = new Autoscaler(args[0], args[1]);
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(autoscaler.getRunnable(), 1, 1, TimeUnit.MINUTES);
  }
}
