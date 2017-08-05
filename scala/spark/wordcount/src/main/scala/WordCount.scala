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

import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark._

import scala.collection.JavaConversions._

import com.google.cloud.bigtable.hbase.BigtableConfiguration

/**
  * Basic WordCount sample of using Cloud Dataproc (managed Apache Spark)
  * to write to Cloud Bigtable.
  */
object WordCount {
  val ColumnFamily = "cf"
  val ColumnFamilyBytes = Bytes.toBytes(ColumnFamily)
  val ColumnNameBytes = Bytes.toBytes("Count")

  val WordCountTableName = "wordcount"

  def createConnection(ProjectId: String, InstanceID: String): Connection = {
    BigtableConfiguration.connect(ProjectId, InstanceID)
  }

  def writeWordCount(word: String, count: Integer, mutator:BufferedMutator) = {
      mutator.mutate(new Put(Bytes.toBytes(word)).
      addColumn(ColumnFamilyBytes,
        ColumnNameBytes,
        Bytes.toBytes(count)))
  }

  def createTableIfNotExists(connection: Connection) = {
    val tableName = TableName.valueOf(WordCountTableName)
    try {
      val admin = connection.getAdmin()
      if (!admin.tableExists(tableName)) {
        val tableDescriptor = new HTableDescriptor(tableName)
        tableDescriptor.addFamily(
          new HColumnDescriptor(ColumnFamily))
        admin.createTable(tableDescriptor)
      }
      admin.close()
    }
  }

  def deleteTable(connection: Connection) = {
    val tableName = TableName.valueOf(WordCountTableName)
    try {
      val admin = connection.getAdmin()
      if (admin.tableExists(tableName)) {
        admin.deleteTable(tableName)
      }
      admin.close()
    }
  }

  def main(args: Array[String]) {
    val ProjectId = args(0)
    val InstanceID = args(1)
    val WordCountTableName = args(2)
    val File = args(3)

    val sc = new SparkContext()

    val createTableConnection = createConnection(ProjectId, InstanceID)
    try {
      createTableIfNotExists(createTableConnection)
    } finally {
      createTableConnection.close()
    }

    val wordCounts = sc.textFile(File).
      flatMap(_.split(" ")).
      filter(_!="").map((_,1)).
      reduceByKey((a,b) => a+b)

    wordCounts.foreachPartition {
      partition => {
        partition.foreach {
          wordcount => {
            val partitionConnection = createConnection(ProjectId, InstanceID)
            val table = TableName.valueOf(WordCountTableName)
            val mutator = partitionConnection.getBufferedMutator(table)
            try {
              partition.foreach {
                wordCount => {
                  val (word, count) = wordCount
                  try {
                    writeWordCount(word, count, mutator)
                  } catch {
                    case retries_e: RetriesExhaustedWithDetailsException => {
                      retries_e.getCauses().foreach(_.printStackTrace)
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
    }

    val deleteConnection = createConnection(ProjectId, InstanceID)
    try {
      deleteTable(deleteConnection)
    } finally {
      deleteConnection.close()
    }

  }
}
