/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package com.example.bigtable.dataflow.pardo;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import com.google.cloud.bigtable.hbase1_0.BigtableConnection;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;

/**
 * A starter example for writing Google Cloud Dataflow ParDos using Cloud Bigtable.
 * <p>
 * The example takes two strings, converts them to their upper-case representation and writes them
 * to Cloud Bigtable.
 * <p>
 * This pipeline needs to be configured with four command line options for Cloud Bigtable:
 * </p>
 * <ul>
 * <li>--bigtableProjectId=[bigtable project]
 * <li>--bigtableClusterId=[bigtable cluster id]
 * <li>--bigtableZoneId=[bigtable zoneId]
 * <li>--bigtableTableId=[bigtable tableId]
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the four
 * Cloud Bigtable parameters from your favorite development environment. You also need to configure
 * the GOOGLE_APPLICATION_CREDENTIALS environment variable as per the "How the Application Default
 * Credentials work" in
 * https://developers.google.com/identity/protocols/application-default-credentials.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also
 * specify the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration. The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
@SuppressWarnings("serial")
public class HelloWorldBigtablePardo {
  private static final byte[] FAMILY = Bytes.toBytes("cf");
  private static final byte[] QUALIFIER = Bytes.toBytes("qualifier");
  private static final byte[] VALUE = Bytes.toBytes("value_" + (60 * Math.random()));

  /**
   * Writes the element in the context to Bigtable.
   *
   */
  public static class HBaseBigtableWriter extends DoFn<String, Void> {
    private Connection conn;
    private BufferedMutator mutator;

    public HBaseBigtableWriter() {
    }

    @Override
    public void startBundle(DoFn<String, Void>.Context c) throws Exception {
      super.startBundle(c);
      CloudBigtableOptions options = c.getPipelineOptions().as(CloudBigtableOptions.class);
      Configuration config = new Configuration();
      config.set(BigtableOptionsFactory.PROJECT_ID_KEY, options.getBigtableProjectId());
      config.set(BigtableOptionsFactory.ZONE_KEY, options.getBigtableZoneId());
      config.set(BigtableOptionsFactory.CLUSTER_KEY, options.getBigtableClusterId());

      conn = new BigtableConnection(config);
      mutator = conn.getBufferedMutator(TableName.valueOf(options.getBigtableTableId()));
    }

    @Override
    public void processElement(DoFn<String, Void>.ProcessContext c) throws Exception {
      mutator.mutate(new Put(c.element().getBytes()).addColumn(FAMILY, QUALIFIER, VALUE));
    }

    @Override
    public void finishBundle(DoFn<String, Void>.Context c) throws Exception {
      try {
        mutator.close();
      } catch (RetriesExhaustedWithDetailsException e) {
        List<Throwable> causes = e.getCauses();
        if (causes.size() == 1) {
          throw (Exception) causes.get(0);
        } else {
          throw e;
        }
      }
      conn.close();
      super.finishBundle(c);
    }
  }

  public interface CloudBigtableOptions extends PipelineOptions {

    @Description("The Google Cloud projectId for the Cloud Bigtable cluster.")
    String getBigtableProjectId();

    void setBigtableProjectId(String bigtableProjectId);

    @Description("The Cloud Bigtable cluster id.")
    String getBigtableClusterId();

    void setBigtableClusterId(String bigtableClusterId);

    @Description("The Google Cloud zoneId in which the cluster resides.")
    String getBigtableZoneId();

    void setBigtableZoneId(String bigtableZoneId);

    @Description("Optional - The id of the Cloud Bigtable table." )
    String getBigtableTableId();

    void setBigtableTableId(String bigtableTableId);
  }

  /**
   * <p>Creates a Cloud Dataflow pipeline that creates the following chain:</p>
   * <ol>
   *   <li> Puts an array of "Hello", "World" into the Pipeline
   *   <li> Performs a Cloud Bigtable Put on the items in the
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   *   when running via managed resource in Google Cloud Platform.  Those options should be omitted
   *   for LOCAL runs.  The last four arguments are to configure the Bigtable connection.
   *        --runner=BlockingDataflowPipelineRunner
   *        --project=[dataflow project] \\
   *        --stagingLocation=gs://[your google storage bucket] \\
   *        --bigtableProjectId=[bigtable project] \\
   *        --bigtableClusterId=[bigtable cluster id] \\
   *        --bigtableZoneId=[bigtable zone]
   *        --bigtableTable=[bigtable tableName]
   */
  public static void main(String[] args) {
    CloudBigtableOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CloudBigtableOptions.class);
    Pipeline p = Pipeline.create(options);

    p
        .apply(Create.of("Hello", "World"))
        .apply(ParDo.of(new HBaseBigtableWriter()));

    p.run();
  }
}
