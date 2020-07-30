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

import org.scalatest.{BeforeAndAfterAll, Suite}

import org.apache.spark.{SparkConf, SparkContext}

trait SparkSpec extends BeforeAndAfterAll {
  this: Suite =>

  private var _sc: SparkContext = _

  override def beforeAll(): Unit = {
    super.beforeAll()

    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName(this.getClass.getSimpleName)
    // Workaround for a bug in TableOutputFormat in HBase 1.6.0
    // See https://stackoverflow.com/a/51959451/1305344
    conf.set("spark.hadoop.validateOutputSpecs", "false")

    sparkConfig.foreach { case (k, v) => conf.setIfMissing(k, v) }

    _sc = new SparkContext(conf)

    sc.setLogLevel("WARN")
  }

  def sparkConfig: Map[String, String] = Map.empty

  override def afterAll(): Unit = {
    if (_sc != null) {
      _sc.stop()
      _sc = null
    }
    super.afterAll()
  }

  implicit def sc: SparkContext = _sc
}
