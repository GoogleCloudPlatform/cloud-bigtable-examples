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

import java.util.Map;

import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableTableConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.io.BigQueryIO;
import com.google.api.services.bigquery.model.TableRow;

/**
 * <p>
 * This is an example of Bigtable with Dataflow using a Sink.
 * The main method adds the data from BigQuery into the pipeline,
 * converts them to Puts, and then writes the Puts to a Bigtable table of your choice.
 * In this example, the item key is auto-generated using UUID.
 * This has to be designed/modified according to the access pattern in your application.
 * Prerequisites: Create a bigtable instance/cluster, and create the table. Expecting column family 'cf'
 * create 'bigquery_to_bigtable_test','cf'
 * </p>
 */

public class BigQueryBigtableTransfer {
  private static final byte[] FAMILY = Bytes.toBytes("cf");

  static final DoFn<TableRow, Mutation> MUTATION_TRANSFORM = new DoFn<TableRow, Mutation>() {
    private static final long serialVersionUID = 1L;

    @Override
    public void processElement(DoFn<TableRow, Mutation>.ProcessContext c) throws Exception {

      TableRow row = c.element();

      //Use UUID for each HBase item's row key
      Put p = new Put(java.util.UUID.randomUUID().toString().getBytes());

      for (Map.Entry<String, Object> field : row.entrySet()) {
        p.addColumn(FAMILY, field.getKey().getBytes(), ((String) field.getValue()).getBytes());
      }
      c.output(p);

    }
  };


  /**
   * Options supported by {@link BigQueryBigtableTransfer}.
   *
   * <p>Defining your own configuration options. Here, you can add your own arguments
   * to be processed by the command-line parser, and specify default values for them. You can then
   * access the options values in your pipeline code.
   *
   * <p>Inherits standard configuration options.
   */
  public interface BigQueryBigtableTransferOptions extends CloudBigtableOptions {
    @Description("Query for BigQuery")
    String getBqQuery();
    void setBqQuery(String value);

  }

  /**
   * <p>Creates a dataflow pipeline that creates the following chain:</p>
   * <ol>
   *   <li> Gets the records into the Pipeline
   *   <li> Creates Puts from each of the records
   *   <li> Performs a Bigtable Put on the records
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   *   when running via managed resource in Google Cloud Platform.  Those options should be omitted
   *   for LOCAL runs.  The last four arguments are to configure the Bigtable connection.
   *        --runner=BlockingDataflowPipelineRunner
   *        --project=[dataflow project] \\
   *        --stagingLocation=gs://[your google storage bucket] \\
   *        --bigtableProject=[bigtable project] \\
   *        --bigtableInstanceId=[bigtable instance id] \\
   *        --bigtableTableId=[bigtable tableName]
   */

  public static void main(String[] args) {
    // CloudBigtableOptions is one way to retrieve the options.  It's not required.
    BigQueryBigtableTransferOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigQueryBigtableTransferOptions.class);

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    CloudBigtableTableConfiguration config =
        CloudBigtableTableConfiguration.fromCBTOptions(options);

    Pipeline p = Pipeline.create(options);
    // This sets up serialization for Puts and Deletes so that Dataflow can potentially move them
    // through the network
    CloudBigtableIO.initializeForWrite(p);

    p
       .apply(BigQueryIO.Read.named("ReadSourceTable").fromQuery(options.getBqQuery()).usingStandardSql())
       .apply(ParDo.of(MUTATION_TRANSFORM))
       .apply(CloudBigtableIO.writeToTable(config));

    p.run();

  }
}
