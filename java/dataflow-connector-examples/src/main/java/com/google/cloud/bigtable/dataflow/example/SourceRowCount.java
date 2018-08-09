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
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Count;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableScanConfiguration;

/**
 * <p>This is a Source example of Cloud Bigtable with Dataflow. The main method outs the
 * words "Hello" and "World" into the pipeline, converts them to Puts, and then writes the Puts to a
 * Cloud Bigtable table of your choice.</p>
 *
 * <p>
 * The example takes two strings, converts them to their upper-case representation and writes them
 * to Cloud Bigtable.
 * <p>
 * This pipeline needs to be configured with four command line options for bigtable:
 * </p>
 * <ul>
 * <li>--bigtableProjectId=[bigtable project]</li>
 * <li>--bigtableInstanceId=[bigtable instance id]</li>
 * <li>--bigtableTableId=[bigtable tableName]</li>
 * </ul>
 * <p>
 * To run this starter example locally using DirectPipelineRunner, just execute it with the four
 * Cloud Bigtable parameters from your favorite development environment.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also
 * specify the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration. The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
public class SourceRowCount {

  /**
   * Options needed for running the pipelne. It needs a
   */
  public static interface CountOptions extends CloudBigtableOptions {

    void setResultLocation(String resultLocation);

    String getResultLocation();
  }

  // Converts a Long to a String so that it can be written to a file.
  static DoFn<Long, String> stringifier = new DoFn<Long, String>() {
    private static final long serialVersionUID = 1L;

    @ProcessElement
    public void processElement(DoFn<Long, String>.ProcessContext context) throws Exception {
      context.output(context.element().toString());
    }
  };

  public static void main(String[] args) {
    CountOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CountOptions.class);
    String PROJECT_ID = options.getBigtableProjectId();
    String INSTANCE_ID = options.getBigtableInstanceId();
    String TABLE_ID = options.getBigtableTableId();

    // [START bigtable_dataflow_connector_scan_config]
    Scan scan = new Scan();
    scan.setCacheBlocks(false);
    scan.setFilter(new FirstKeyOnlyFilter());

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    // You can supply an optional Scan() to filter the rows that will be read.
    CloudBigtableScanConfiguration config =
        new CloudBigtableScanConfiguration.Builder()
            .withProjectId(PROJECT_ID)
            .withInstanceId(INSTANCE_ID)
            .withTableId(TABLE_ID)
            .withScan(scan)
            .build();

    Pipeline p = Pipeline.create(options);

    p.apply(Read.from(CloudBigtableIO.read(config)))
        .apply(Count.<Result>globally())
        .apply(ParDo.of(stringifier))
        .apply(TextIO.write().to(options.getResultLocation()));
    // [END bigtable_dataflow_connector_scan_config]

    p.run().waitUntilFinish();

    // Once this is done, you can get the result file via "gsutil cp <resultLocation>-00000-of-00001"
  }
}
