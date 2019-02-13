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

package com.example.bigtable.spark.shc

object BigtableSource extends App {

  val tableName = args(0)
  val cat =
    s"""{
       |"table":{"namespace":"default", "name":"$tableName", "tableCoder":"PrimitiveType"},
       |"rowkey":"key",
       |"columns":{
       |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
       |"col1":{"cf":"cf1", "col":"col1", "type":"boolean"},
       |"col2":{"cf":"cf2", "col":"col2", "type":"double"},
       |"col3":{"cf":"cf3", "col":"col3", "type":"int"}
       |}
       |}""".stripMargin
  val appName = getClass.getSimpleName.replace("$", "")

  import org.apache.spark.sql.SparkSession

  val spark = SparkSession
    .builder
    .appName(appName)
    .getOrCreate

  import spark.implicits._

  val records = (0 to 255).map(BigtableRecord.apply).toDF

  import org.apache.spark.sql.execution.datasources.hbase.HBaseTableCatalog

  val format = "org.apache.spark.sql.execution.datasources.hbase"
  val opts = Map(
    HBaseTableCatalog.tableCatalog -> cat,
    HBaseTableCatalog.newTable -> "5")
  records
    .write
    .options(opts)
    .format(format)
    .save

  val df = spark
    .read
    .option(HBaseTableCatalog.tableCatalog, cat)
    .format(format)
    .load
  // Optimization: cache the dataset as it is going to be used in multiple queries
  // Please note that the caching is lazy, but the following show action should trigger it
  df.cache
  df.show

  df.filter($"col0" <= "row005")
    .select($"col0", $"col1")
    .show
  df.filter($"col0" === "row005" || $"col0" <= "row005")
    .select($"col0", $"col1")
    .show
  df.filter($"col0" > "row250")
    .select($"col0", $"col1")
    .show

  df.createOrReplaceTempView("table1")
  spark
    .sql("select count(col1) from table1 where col0 < 'row050'")
    .show

  spark.stop
}

case class BigtableRecord(
  col0: String,
  col1: Boolean,
  col2: Double,
  col3: Int)

object BigtableRecord {
  def apply(i: Int): BigtableRecord = {
    val s = s"""row${"%03d".format(i)}"""
    BigtableRecord(s,
      i % 2 == 0,
      i.toDouble,
      i)
  }
}
