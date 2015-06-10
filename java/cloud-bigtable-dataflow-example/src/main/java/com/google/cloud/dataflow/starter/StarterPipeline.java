/*
 * Copyright (C) 2015 Google Inc. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed
 * to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.google.cloud.dataflow.starter;

import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import com.google.cloud.bigtable.hbase1_0.BigtableConnection;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;

/**
 * A starter example for writing Google Cloud Dataflow programs using Bigtable.
 * <p>
 * The example takes two strings, converts them to their upper-case representation and writes
 * them to Bigtable.
 * <p>
 * This pipeline needs to be configured with four command line options for bigtable:
 * </p>
 * <ul>
 *  <li> --bigtableProject=[bigtable project]
 *  <li> --bigtableClusterId=[bigtable cluster id]
 *  <li> --bigtableZone=[bigtable zone]
 *  <li> --bigtableTable=[bigtable tableName]
 * <p> 
 * To run this starter example locally using DirectPipelineRunner, just execute it with the four
 * Bigtable parameters from your favorite development environment.  You also need to configure
 * the GOOGLE_APPLICATION_CREDENTIALS environment variable as per the "How the Application Default 
 * Credentials work" in https://developers.google.com/identity/protocols/application-default-credentials.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also specify
 * the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration.  The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
@SuppressWarnings("serial")
public class StarterPipeline {
  private static final byte[] FAMILY = Bytes.toBytes("cf");
  private static final byte[] QUALIFIER = Bytes.toBytes("qualifier");

  private static final byte[] VALUE = Bytes.toBytes("value_" + new Date().getMinutes());

  /**
   * Writes the element in the context to Bigtable.
   *
   */
  public static class HBaseBigtableWriter extends DoFn<String, Void> {
    private Connection conn;
    private BufferedMutator mutator;
    private final String project;
    private final String clusterId;
    private final String zone;
    private final String tableName;

    public HBaseBigtableWriter(String project, String clusterId, String zone, String tableName) {
      this.project = project;
      this.clusterId = clusterId;
      this.zone = zone;
      this.tableName = tableName;
    }

    @Override
    public void startBundle(DoFn<String, Void>.Context c) throws Exception {
      super.startBundle(c);
      Configuration config = new Configuration();
      config.set(BigtableOptionsFactory.PROJECT_ID_KEY, project);
      config.set(BigtableOptionsFactory.ZONE_KEY, zone);
      config.set(BigtableOptionsFactory.CLUSTER_KEY, clusterId);
      conn = new BigtableConnection(config);
      mutator = conn.getBufferedMutator(TableName.valueOf(tableName));
    }

    @Override
    public void processElement(DoFn<String, Void>.ProcessContext c) throws Exception {
      Put put = new Put(c.element().getBytes()).addColumn(FAMILY, QUALIFIER, VALUE);
      mutator.mutate(put);
    }
    
    @Override
    public void finishBundle(DoFn<String, Void>.Context c) throws Exception {
      mutator.close();
      conn.close();
      super.finishBundle(c);
    }
  }

  public static interface Options extends PipelineOptions {
    /**
     *  Option to tell dataflow to use Alpn on the server side. This is required to get Bigtable to
     *  work.
     */
    @Default.Boolean(true)
    Boolean getUseAlpn();

    void setUseAlpn(Boolean value);
    
    String getBigtableProject();

    void setBigtableProject(String bigtableProject);
    
    String getBigtableClusterId();

    void setBigtableClusterId(String bigtableClusterId);

    String getBigtableZone();

    void setBigtableZone(String bigtableZone);

    String getBigtableTable();

    void setBigtableTable(String bigtableTable);
}

  /**
   * <p>Creates a dataflow pipeline that creates the following chain:</p>
   * <ol>
   *   <li> Puts an array of "Hello", "World" into the Pipeline
   *   <li> Does a conversion of the Pipeline elements to uppercase
   *   <li> Performs a Bigtable Put on the items in the 
   * </ol>
   * 
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   *   when running via managed resource in Google Cloud Platform.  Those options should be omitted
   *   for LOCAL runs.  The last four arguments are to configure the Bigtable connection.
   *        --runner=BlockingDataflowPipelineRunner 
   *        --project=[dataflow project] \\
   *        --stagingLocation=gs://[your google storage bucket] \\
   *        --bigtableProject=[bigtable project] \\
   *        --bigtableClusterId=[bigtable cluster id] \\
   *        --bigtableZone=[bigtable zone]
   *        --bigtableTable=[bigtable tableName]
   */
  public static void main(String[] args) {
    Options options = PipelineOptionsFactory.fromArgs(args).withValidation().as(Options.class);
    Pipeline p = Pipeline.create(options);
    // If you remove this, then the options object will not actually be configured with use alpn. 
    options.setUseAlpn(true);
    HBaseBigtableWriter hBaseBigtableWriter =
        new HBaseBigtableWriter(options.getBigtableProject(), options.getBigtableClusterId(),
            options.getBigtableZone(), options.getBigtableTable());
    p.apply(Create.of("Hello", "World")).apply(ParDo.of(new DoFn<String, String>() {
      @Override
      public void processElement(ProcessContext c) {
        c.output(c.element().toUpperCase());
      }
    })).apply(ParDo.of(hBaseBigtableWriter));

    p.run();
  }
}
