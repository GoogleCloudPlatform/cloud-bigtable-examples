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

import org.apache.spark.sql.execution.datasources.hbase._
import org.apache.spark.sql.SparkSession

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

object BigtableSource {

  def main(args: Array[String]) {
    val tableName = args(0)

    val cat = s"""{
                 |"table":{"namespace":"default", "name":\"$tableName\", "tableCoder":"PrimitiveType"},
                 |"rowkey":"key",
                 |"columns":{
                 |"col0":{"cf":"rowkey", "col":"key", "type":"string"},
                 |"col1":{"cf":"cf1", "col":"col1", "type":"boolean"},
                 |"col2":{"cf":"cf2", "col":"col2", "type":"double"},
                 |"col3":{"cf":"cf3", "col":"col3", "type":"int"}
                 |}
                 |}""".stripMargin

    val spark = SparkSession.builder()
      .appName("HBaseSourceExample")
      .getOrCreate()

    val sc = spark.sparkContext
    val sqlContext = spark.sqlContext

    import sqlContext.implicits._

    val data = (0 to 255).map { i =>
      BigtableRecord(i)
    }

    sc.parallelize(data).toDF.write.options(
      Map(HBaseTableCatalog.tableCatalog -> cat, HBaseTableCatalog.newTable -> "5"))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .save()

    val df = sqlContext
      .read
      .options(Map(HBaseTableCatalog.tableCatalog->cat))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .load()
    df.show
    df.filter($"col0" <= "row005")
      .select($"col0", $"col1").show
    df.filter($"col0" === "row005" || $"col0" <= "row005")
      .select($"col0", $"col1").show
    df.filter($"col0" > "row250")
      .select($"col0", $"col1").show
    df.createOrReplaceTempView("table1")
    val c = sqlContext.sql("select count(col1) from table1 where col0 < 'row050'")
    c.show()

    spark.stop()
  }
}
