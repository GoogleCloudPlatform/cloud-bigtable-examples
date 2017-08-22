/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bigtable.spark.wordcount

import java.util.concurrent.TimeUnit

import com.google.bigtable.repackaged.com.google.cloud.bigtable.config.BigtableOptions
import com.google.cloud.bigtable.hbase.{BigtableConfiguration, BigtableOptionsFactory}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.{HColumnDescriptor, HConstants, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.mapreduce.{TableInputFormat, TableOutputFormat}
import org.apache.hadoop.hbase.client.{Connection, Put}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.mapreduce.Job
import org.apache.spark.{SparkConf}
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Basic WordCount sample of using Cloud Dataproc (managed Apache Spark)
  * to write to Cloud Bigtable.
  */
object WordCountStreaming {
  val ColumnFamily = "cf"
  val ColumnFamilyBytes = Bytes.toBytes(ColumnFamily)
  val ColumnNameBytes = Bytes.toBytes("Count")


  def createConnection(ProjectId: String, InstanceID: String): Connection = {
    BigtableConfiguration.connect(ProjectId, InstanceID)
  }


  /**
    * Sets parameters well-tuned for batch writes.
    * Copied from Cloud Bigtable client configuration for Google Cloud
    * Dataflow.
    * https://github.com/GoogleCloudPlatform/cloud-bigtable-client/blob/bigtable-client-1.0.0-pre2/bigtable-dataflow-parent/bigtable-hbase-dataflow/src/main/java/com/google/cloud/bigtable/dataflow/CloudBigtableConfiguration.java#L159
    * @param config Hadoop config to set options on.
    */
  private def setBatchConfigOptions(config: Configuration) = {
    config.set(BigtableOptionsFactory.BIGTABLE_USE_CACHED_DATA_CHANNEL_POOL, "true")

    // Dataflow should use a different endpoint for data operations than online traffic.
    config.set(BigtableOptionsFactory.BIGTABLE_HOST_KEY, BigtableOptions.BIGTABLE_BATCH_DATA_HOST_DEFAULT)

    config.set(BigtableOptionsFactory.INITIAL_ELAPSED_BACKOFF_MILLIS_KEY, String.valueOf(TimeUnit.SECONDS.toMillis(5)))

    config.set(BigtableOptionsFactory.MAX_ELAPSED_BACKOFF_MILLIS_KEY, String.valueOf(TimeUnit.MINUTES.toMillis(5)))

    // This setting can potentially decrease performance for large scale writes. However, this
    // setting prevents problems that occur when streaming Sources, such as PubSub, are used.
    // To override this behavior, call:
    //    Builder.withConfiguration(BigtableOptionsFactory.BIGTABLE_ASYNC_MUTATOR_COUNT_KEY,
    //                              BigtableOptions.BIGTABLE_ASYNC_MUTATOR_COUNT_DEFAULT);
    config.set(BigtableOptionsFactory.BIGTABLE_ASYNC_MUTATOR_COUNT_KEY, "0")
  }


  /**
    * Create a table in the Cloud Bigtable instance if it doesn't already
    * exit.
    *
    * @param connection A Cloud Bigtable connection
    * @param name       The table name
    */
  def createTableIfNotExists(connection: Connection, name: String) = {
    val tableName = TableName.valueOf(name)
    val admin = connection.getAdmin()
    try {
      if (!admin.tableExists(tableName)) {
        val tableDescriptor = new HTableDescriptor(tableName)
        tableDescriptor.addFamily(
          new HColumnDescriptor(ColumnFamily))
        admin.createTable(tableDescriptor)
      }
    } finally {
      admin.close()
    }
  }

  /**
    * Main entry point for running the WordCount spark job and writing
    * the results to Bigtable.
    *
    * @param projectId  The Google Cloud Project ID
    * @param instanceId The Cloud Bigtable instance name
    * @param tableName  The name of the Cloud Bigtable table to write to
    * @param fileDirectory   The file directory to stream from
    * @param ssc         The Spark streaming context
    */
  def runner(projectId: String,
             instanceId: String,
             tableName: String,
             fileDirectory: String,
             ssc: StreamingContext) = {
    val createTableConnection = createConnection(projectId, instanceId)
    try {
      createTableIfNotExists(createTableConnection, tableName)
    } finally {
      createTableConnection.close()
    }

    var conf = BigtableConfiguration.configure(
      projectId, instanceId)
    conf.set(TableInputFormat.INPUT_TABLE, tableName)
    conf.set(TableOutputFormat.OUTPUT_TABLE, tableName)
    conf.setInt(HConstants.HBASE_CLIENT_OPERATION_TIMEOUT, 60000)
    setBatchConfigOptions(conf)

    // workaround: https://issues.apache.org/jira/browse/SPARK-21549
    conf.set("mapreduce.output.fileoutputformat.outputdir", "/tmp")
    val job = new Job(conf)
    job.setOutputFormatClass(classOf[TableOutputFormat[ImmutableBytesWritable]])
    conf = job.getConfiguration()

    val dStream = ssc.textFileStream(fileDirectory)
    dStream.foreachRDD { rdd =>
      val wordCounts = rdd.flatMap(_.split(" ")).
        filter(_ != "").map((_, 1)).
        reduceByKey((a, b) => a + b)
        .map({
          case (word, count) =>
            (null, new Put(Bytes.toBytes(word))
              .addColumn(ColumnFamilyBytes, ColumnNameBytes, Bytes.toBytes(count)))
        })
        wordCounts.saveAsNewAPIHadoopDataset(conf)
    }
  }

  /**
    * Main program parses command line args and creates Spark context.
    * @param args
    */
  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("WordCount")
    val ProjectId = args(0)
    val InstanceID = args(1)
    val WordCountTableName = args(2)
    val File = args(3)
    val ssc = new StreamingContext(conf, Seconds(1))
    runner(ProjectId, InstanceID, WordCountTableName, File, ssc)
    ssc.start()             // Start the computation
    ssc.awaitTermination()  // Wait for the computation to terminate
  }
}
