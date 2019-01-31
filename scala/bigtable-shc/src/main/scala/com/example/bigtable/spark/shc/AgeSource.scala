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

/**
  * This is a companion example with people name and ages, which may be
  * more concrete and easier to understand than `BigtableSource`.
  */
object AgeSource extends App {

  val tableName = args(0)
  val cat =
    s"""{
       |"table":{"namespace":"default", "name":"$tableName", "tableCoder":"PrimitiveType"},
       |"rowkey":"name",
       |"columns":{
       |"name":{"cf":"rowkey", "col":"name", "type":"string"},
       |"age":{"cf":"age", "col":"age", "type":"int"}
       |}
       |}""".stripMargin
  val appName = getClass.getSimpleName.replace("$", "")

  val spark = SparkSession
    .builder
    .appName(appName)
    .getOrCreate

  import spark.implicits._

  case class PersonRecord(name: String, age: Int)
  val people = Seq(
    PersonRecord("bill", 30),
    PersonRecord("solomon", 27),
    PersonRecord("misha", 30),
    PersonRecord("jeff", 30),
    PersonRecord("les", 27),
    PersonRecord("ramesh", 31),
    PersonRecord("jacek", 45)
  ).toDF

  val format = "org.apache.spark.sql.execution.datasources.hbase"
  val opts = Map(
    HBaseTableCatalog.tableCatalog -> cat,
    HBaseTableCatalog.newTable -> "5")

  people
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

  df.filter($"age" >= "29")
    .select($"name", $"age")
    .show
  df.groupBy("age")
    .count()
    .show

  df.createOrReplaceTempView("peopletable")
  spark
    .sql(s"select avg(age) from peopletable")
    .show

  spark.stop

}
