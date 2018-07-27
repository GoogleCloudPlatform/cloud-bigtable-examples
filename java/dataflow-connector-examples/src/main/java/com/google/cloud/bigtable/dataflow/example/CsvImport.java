/*
 * Copyright Copyright 2018 Google LLC. All Rights Reserved.
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

import com.google.common.base.Preconditions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.beam.sdk.io.TextIO;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This is an example of importing a CSV into Bigtable with Dataflow. The main method adds the rows
 * of the CSV into the pipeline, converts them to Puts, and then writes the Puts to a Bigtable
 * table.
 * </p>
 * This pipeline needs to be configured with command line options:
 * </p>
 * <ul>
 * <li>--headers=[CSV headers]
 * <li>--inputFile=[URI to GCS file]
 * <li>--bigtableProjectId=[bigtable project]
 * <li>--bigtableInstanceId=[bigtable instance id]
 * <li>--bigtableTableId=[bigtable tableName]
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the
 * parameters from your favorite development environment.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also
 * specify the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration. The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
public class CsvImport {

  private static final byte[] FAMILY = Bytes.toBytes("csv");
  private static final Logger LOG = LoggerFactory.getLogger(CsvImport.class);

  static final DoFn<String, Mutation> MUTATION_TRANSFORM = new DoFn<String, Mutation>() {
    @ProcessElement
    public void processElement(DoFn<String, Mutation>.ProcessContext c) throws Exception {
      try {
        String[] headers = c.getPipelineOptions().as(BigtableCsvOptions.class).getHeaders()
            .split(",");
        String[] values = c.element().split(",");
        Preconditions.checkArgument(headers.length == values.length);

        byte[] rowkey = Bytes.toBytes(values[0]);
        byte[][] headerBytes = new byte[headers.length][];
        for (int i = 0; i < headers.length; i++) {
          headerBytes[i] = Bytes.toBytes(headers[i]);
        }

        Put row = new Put(rowkey);
        long timestamp = System.currentTimeMillis();
        for (int i = 1; i < values.length; i++) {
          row.addColumn(FAMILY, headerBytes[i], timestamp, Bytes.toBytes(values[i]));
        }
        c.output(row);
      } catch (Exception e) {
        LOG.error("Failed to process input {}", c.element(), e);
        throw e;
      }

    }
  };

  public static interface BigtableCsvOptions extends CloudBigtableOptions {

    @Description("The headers for the CSV file.")
    String getHeaders();

    void setHeaders(String headers);

    @Description("The Cloud Storage path to the CSV file..")
    String getInputFile();

    void setInputFile(String location);
  }


  /**
   * <p>Creates a dataflow pipeline that reads a file and creates the following chain:</p>
   * <ol>
   * <li> Put each row of the CSV into the Pipeline.
   * <li> Creates a Put object for each row.
   * <li> Write the Put object to Bigtable.
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   * when running via managed resource in Google Cloud Platform.  Those options should be omitted
   * for LOCAL runs.  The next two are to configure your CSV file. And the last four arguments are
   * to configure the Bigtable connection. --runner=BlockingDataflowPipelineRunner
   * --project=[dataflow project] \\ --stagingLocation=gs://[your google storage bucket] \\
   * --headers=[comma separated list of headers] \\ --inputFile=gs://[your google storage object] \\
   * --bigtableProject=[bigtable project] \\ --bigtableInstanceId=[bigtable instance id] \\
   * --bigtableTableId=[bigtable tableName]
   */

  public static void main(String[] args) {
    BigtableCsvOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigtableCsvOptions.class);

    CloudBigtableTableConfiguration config =
        new CloudBigtableTableConfiguration.Builder()
            .withProjectId(options.getBigtableProjectId())
            .withInstanceId(options.getBigtableInstanceId())
            .withTableId(options.getBigtableTableId())
            .build();

    Pipeline p = Pipeline.create(options);

    p.apply("ReadMyFile", TextIO.read().from(options.getInputFile()))
        .apply("TransformParsingsToBigtable", ParDo.of(MUTATION_TRANSFORM))
        .apply("WriteToBigtable", CloudBigtableIO.writeToTable(config));

    p.run().waitUntilFinish();
  }
}
