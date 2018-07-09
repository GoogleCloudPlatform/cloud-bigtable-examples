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
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.io.TextIO;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * This is an example of importing a CSV into Bigtable with Dataflow. The main method adds the
 * rows of the CSV into the pipeline, converts them to Puts, and then writes the Puts to a
 * Bigtable table.
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
 * To run this starter example locally using DirectPipelineRunner, just execute it with the parameters
 * from your favorite development environment.
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
    private static final Logger LOG = LoggerFactory.getLogger(ParseEventFn.class);

    static final DoFn<String[], Mutation> MUTATION_TRANSFORM = new DoFn<String[], Mutation>() {
        @ProcessElement
        public void processElement(DoFn<String[], Mutation>.ProcessContext c) throws Exception {
            try {
                String[] headers = c.getPipelineOptions().as(BigtableCsvOptions.class).getHeaders().split(",");

                byte[] rowId = Bytes.toBytes(c.element()[0]);
                Put row = new Put(rowId);
                for (int i = 1; i < c.element().length; i++) {
                    row.addColumn(FAMILY, Bytes.toBytes(headers[i]), Bytes.toBytes(c.element()[i]));
                }
                c.output(row);
            } catch (Exception e) {
                LOG.error("Failed to process input {}", c.element(), e);
            }

        }
    };

    static class ParseEventFn extends DoFn<String, String[]> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            c.output(c.element().split(","));
        }
    }

    public static interface BigtableCsvOptions extends CloudBigtableOptions {
        String getHeaders();

        void setHeaders(String headers);

        String getInputFile();

        void setInputFile(String location);
    }


    /**
     * <p>Creates a dataflow pipeline that reads a file and creates the following chain:</p>
     * <ol>
     * <li> Put each row of the CSV into the Pipeline.
     * <li> Transform each row into an array of values.
     * <li> Creates a Put object for each array.
     * <li> Write the Put object to Bigtable.
     * </ol>
     *
     * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
     *             when running via managed resource in Google Cloud Platform.  Those options should be omitted
     *             for LOCAL runs.  The next two are to configure your CSV file. And the last four arguments are to
     *             configure the Bigtable connection.
     *             --runner=BlockingDataflowPipelineRunner
     *             --project=[dataflow project] \\
     *             --stagingLocation=gs://[your google storage bucket] \\
     *             --headers=[comma separated list of headers] \\
     *             --inputFile=gs://[your google storage object] \\
     *             --bigtableProject=[bigtable project] \\
     *             --bigtableInstanceId=[bigtable instance id] \\
     *             --bigtableTableId=[bigtable tableName]
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

        PCollection<String> lines = p.apply("ReadMyFile", TextIO.read().from(options.getInputFile()));

        lines
                .apply("ParseEvent", ParDo.of(new ParseEventFn()))
                .apply("TransformParsingsToBigtable", ParDo.of(MUTATION_TRANSFORM))
                .apply("WriteToBigtable", CloudBigtableIO.writeToTable(config));

        p.run().waitUntilFinish();
    }
}
