/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bigtable.spark.wordcount

import com.google.cloud.bigtable.hbase.BigtableConfiguration
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client.{BufferedMutator, Connection, Put, RetriesExhaustedWithDetailsException}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkContext

/**
  * Basic WordCount sample of using Cloud Dataproc (managed Apache Spark)
  * to write to Cloud Bigtable.
  */
object WordCount {
  val ColumnFamily = "cf"
  val ColumnFamilyBytes = Bytes.toBytes(ColumnFamily)
  val ColumnNameBytes = Bytes.toBytes("Count")

  def createConnection(ProjectId: String, InstanceID: String): Connection = {
    BigtableConfiguration.connect(ProjectId, InstanceID)
  }

  /**
    * Write the count of the word to Cloud Bigtable using the word itself
    * as the row key.
    *
    * @param word    The word we are writing the count of, and row key
    * @param count   The frequency of the word in the document
    * @param mutator A Cloud Bigtable mutator for writing
    */
  def writeWordCount(word: String, count: Integer, mutator: BufferedMutator) = {
    mutator.mutate(new Put(Bytes.toBytes(word)).
      addColumn(ColumnFamilyBytes,
        ColumnNameBytes,
        Bytes.toBytes(count)))
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
    * @param fileName   The file (local or gcs) to sort
    * @param sc         The Spark context
    */
  def runner(projectId: String, instanceId: String,
             tableName: String, fileName: String,
             sc: SparkContext) = {
    val createTableConnection = createConnection(projectId, instanceId)
    try {
      createTableIfNotExists(createTableConnection, tableName)
    } finally {
      createTableConnection.close()
    }

    val wordCounts = sc.textFile(fileName).
      flatMap(_.split(" ")).
      filter(_ != "").map((_, 1)).
      reduceByKey((a, b) => a + b)

    // Create a per-partition connection to ensure each node has a
    // connection (partitions are on at most 1 node).
    wordCounts.foreachPartition {
      partition => {
        val partitionConnection = createConnection(projectId, instanceId)
        val table = TableName.valueOf(tableName)
        val mutator = partitionConnection.getBufferedMutator(table)
        try {
          partition.foreach {
            wordCount => {
              val (word, count) = wordCount
              try {
                writeWordCount(word, count, mutator)
              } catch {
                case retries_e: RetriesExhaustedWithDetailsException => {
                  println("Retries: " + retries_e.getClass)
                  throw retries_e.getCauses().get(0)
                }
              }
            }
          }
        } finally {
          mutator.close()
          partitionConnection.close()
        }
      }
    }
  }

  /**
    * Main program parses command line args and creates Spark context.
    * @param args
    */
  def main(args: Array[String]) {
    val ProjectId = args(0)
    val InstanceID = args(1)
    val WordCountTableName = args(2)
    val File = args(3)
    val sc = new SparkContext()
    runner(ProjectId, InstanceID, WordCountTableName, File, sc)
  }
}
