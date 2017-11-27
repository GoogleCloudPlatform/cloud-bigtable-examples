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

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.execution.datasources.hbase.HBaseTableCatalog

case class PersonRecord(
                           name: String,
                           age: Int)

/**
  * This is a companion example with people name and ages, which may be
  * more concrete and easier to understand than `BigtableSource`.
  */
object AgeSource {

  def main(args: Array[String]) {
    val tableName = args(0)

    val cat = s"""{
                 |"table":{"namespace":"default", "name":\"$tableName\", "tableCoder":"PrimitiveType"},
                 |"rowkey":"name",
                 |"colum-ns":{
                 |"name":{"cf":"rowkey", "col":"name", "type":"string"},
                 |"age":{"cf":"age", "col":"age", "type":"int"}
                 |}
                 |}""".stripMargin

    val spark = SparkSession.builder()
      .appName("PersonExample")
      .getOrCreate()

    val sc = spark.sparkContext
    val sqlContext = spark.sqlContext

    import sqlContext.implicits._

    val people = Seq(
      ("bill", 30),
      ("solomon", 27),
      ("misha", 30),
      ("jeff", 30),
      ("les", 27),
      ("ramesh", 31)
    ).toDF("name", "age")

    people.write.options(
      Map(HBaseTableCatalog.tableCatalog -> cat, HBaseTableCatalog.newTable -> "5"))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .save()

    val df = sqlContext
      .read
      .options(Map(HBaseTableCatalog.tableCatalog->cat))
      .format("org.apache.spark.sql.execution.datasources.hbase")
      .load()
    df.show
    df.filter($"age" >= "29")
      .select($"name", $"age").show
    df.groupBy("age").count().show()
    df.createOrReplaceTempView("peopletable")
    sqlContext.sql(
      s"select avg(age) from peopletable"
    ).show
    spark.stop()
  }

}
