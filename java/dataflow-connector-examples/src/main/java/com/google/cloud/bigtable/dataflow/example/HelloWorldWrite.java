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
package com.google.cloud.bigtable.dataflow.example;

import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableTableConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;

/**
 * <p>
 * This is a "Hello World" example of Bigtable with Dataflow using a Sink. The main method add the
 * words "Hello" and "World" into the pipeline, converts them to Puts, and then writes the Puts to a
 * Bigtable table of your choice.
 * </p>
 * <p>
 * The example takes two strings, converts them to their upper-case representation and writes them
 * to Bigtable.
 * <p>
 * This pipeline needs to be configured with four command line options for bigtable:
 * </p>
 * <ul>
 * <li>--bigtableProject=[bigtable project]
 * <li>--bigtableClusterId=[bigtable cluster id]
 * <li>--bigtableZone=[bigtable zone]
 * <li>--bigtableTable=[bigtable tableName]
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the four
 * Bigtable parameters from your favorite development environment. You also need to configure the
 * GOOGLE_APPLICATION_CREDENTIALS environment variable as per the "How the Application Default
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
public class HelloWorldWrite {
  private static final byte[] FAMILY = Bytes.toBytes("cf");
  private static final byte[] QUALIFIER = Bytes.toBytes("qualifier");

  // This is a random value so that there will be some changes in the table
  // each time the job runs.
  private static final byte[] VALUE = Bytes.toBytes("value_" + (60 * Math.random()));

  static final DoFn<String, Mutation> MUTATION_TRANSFORM = new DoFn<String, Mutation>() {
    private static final long serialVersionUID = 1L;

    @Override
    public void processElement(DoFn<String, Mutation>.ProcessContext c) throws Exception {
      c.output(new Put(c.element().getBytes()).addColumn(FAMILY, QUALIFIER, VALUE));
    }
  };

  /**
   * <p>Creates a dataflow pipeline that creates the following chain:</p>
   * <ol>
   *   <li> Puts an array of "Hello", "World" into the Pipeline
   *   <li> Creates Puts from each of the words in the array
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
    // CloudBigtableOptions is one way to retrieve the options.  It's not required.
    CloudBigtableOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CloudBigtableOptions.class);

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    CloudBigtableTableConfiguration config =
        CloudBigtableTableConfiguration.fromCBTOptions(options);

    Pipeline p = Pipeline.create(options);
    // This sets up serialization for Puts and Deletes so that Dataflow can potentially move them
    // through the network
    CloudBigtableIO.initializeForWrite(p);

    p
       .apply(Create.of("Hello", "World"))
       .apply(ParDo.of(MUTATION_TRANSFORM))
       .apply(CloudBigtableIO.writeToTable(config));

    p.run();
  }
}
