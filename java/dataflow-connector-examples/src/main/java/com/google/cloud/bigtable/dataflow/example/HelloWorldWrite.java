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

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;


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
 * This pipeline needs to be configured with three command line options for bigtable:
 * </p>
 * <ul>
 * <li>--bigtableProjectId=[bigtable project]</li>
 * <li>--bigtableInstanceId=[bigtable instance id]</li>
 * <li>--bigtableTableId=[bigtable tableName]</li>
 * </ul>
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the three
 * Bigtable parameters from your favorite development environment.
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


  // [START bigtable_dataflow_connector_process_element]
  static final DoFn<String, Mutation> MUTATION_TRANSFORM = new DoFn<String, Mutation>() {
    private static final long serialVersionUID = 1L;

    @ProcessElement
    public void processElement(DoFn<String, Mutation>.ProcessContext c) throws Exception {
      c.output(new Put(c.element().getBytes()).addColumn(FAMILY, QUALIFIER, VALUE));
    }
  };
  // [END bigtable_dataflow_connector_process_element]

  /**
   * <p>Creates a dataflow pipeline that creates the following chain:</p>
   * <ol>
   * <li>Puts an array of "Hello", "World" into the Pipeline</li>
   * <li>Creates Puts from each of the words in the array</li>
   * <li>Performs a Cloud Bigtable Put on the items in the</li>
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline. The first three are required
   * when running via managed resource in Google Cloud Platform. Those options should be omitted
   * for LOCAL runs. The last four arguments are to configure the Cloud Bigtable connection.
   * <code>--runner=BlockingDataflowPipelineRunner --project=[dataflow project] \\
   * --stagingLocation=gs://[your google storage bucket] \\ --bigtableProject=[bigtable project] \\
   * --bigtableInstanceId=[bigtable instance id] \\ --bigtableTableId=[bigtable tableName]
   * </code>
   */

  public static void main(String[] args) {
    // [START bigtable_dataflow_connector_create_pipeline]
    CloudBigtableOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CloudBigtableOptions.class);
    Pipeline p = Pipeline.create(options);
    // [END bigtable_dataflow_connector_create_pipeline]

    String PROJECT_ID = options.getBigtableProjectId();
    String INSTANCE_ID = options.getBigtableInstanceId();
    String TABLE_ID = options.getBigtableTableId();

    // [START bigtable_dataflow_connector_config]
    CloudBigtableTableConfiguration config =
        new CloudBigtableTableConfiguration.Builder()
            .withProjectId(PROJECT_ID)
            .withInstanceId(INSTANCE_ID)
            .withTableId(TABLE_ID)
            .build();
    // [END bigtable_dataflow_connector_config]

    // [START bigtable_dataflow_connector_write_helloworld]
    p
        .apply(Create.of("Hello", "World"))
        .apply(ParDo.of(MUTATION_TRANSFORM))
        .apply(CloudBigtableIO.writeToTable(config));
    // [END bigtable_dataflow_connector_write_helloworld]

    p.run().waitUntilFinish();
  }
}
