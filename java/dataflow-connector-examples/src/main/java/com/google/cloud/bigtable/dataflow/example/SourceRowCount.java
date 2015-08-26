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

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableScanConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.Read;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.Count;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;

/**
 * <p>This is a Source example of Bigtable with Dataflow. The main method outs the
 * words "Hello" and "World" into the pipeline, converts them to Puts, and then writes the Puts to a
 * Bigtable table of your choice.</p>
 *
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
public class SourceRowCount {

  /**
   * Options needed for running the pipelne.  It needs a
   *
   */
  public static interface CountOptions extends CloudBigtableOptions {
    void setResultLocation(String resultLocation);
    String getResultLocation();
  }

  // Converts a Long to a String so that it can be written to a file.
  static DoFn<Long, String> stringifier = new DoFn<Long, String>() {
    private static final long serialVersionUID = 1L;

    @Override
    public void processElement(DoFn<Long, String>.ProcessContext context) throws Exception {
      context.output(context.element().toString());
    }
  };

  public static void main(String[] args) {
    CountOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CountOptions.class);

    // See the hbase hadoop job at
    // https://github.com/apache/hbase/blob/master/hbase-server/src/main/java/org/apache/hadoop/hbase/mapreduce/RowCounter.java#L151
    // for more ways to configure this scan.
    Scan scan = new Scan();
    scan.setCacheBlocks(false);
    scan.setFilter(new FirstKeyOnlyFilter());

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    // You can supply an optional Scan() to filter the rows that will be read.
    CloudBigtableScanConfiguration config = CloudBigtableScanConfiguration.fromCBTOptions(options);

    Pipeline p = Pipeline.create(options);

    p
       .apply(Read.from(CloudBigtableIO.read(config)))
       .apply(Count.<Result>globally())
       .apply(ParDo.of(stringifier))
       .apply(TextIO.Write.to(options.getResultLocation()));

    p.run();

    // Once this is done, you can get the result file via "gsutil cp <resultLocation>-00000-of-00001"
  }
}
