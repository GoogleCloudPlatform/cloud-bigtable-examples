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

import org.apache.hadoop.hbase.client.{Scan}
import org.apache.hadoop.hbase.{TableName}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers}

/**
  * Word count integration test. While the Spark cluster is spun up locally
  * and programmatically, this test requires a live Cloud Bigtable cluster to
  * exist.
  */
class WordCountIT extends FlatSpec with BeforeAndAfterEach with BeforeAndAfterAll with SparkSpec with Matchers {

  val ProjectId = sys.env("GOOGLE_CLOUD_PROJECT")
  val BigTableInstance = sys.env("CLOUD_BIGTABLE_INSTANCE")
  val WordCountTableName= "WordCount-Test-Scratch-Table"

  def fixture =
    new {
      val connection = WordCount.createConnection(ProjectId, BigTableInstance)
      val tableName = TableName.valueOf(WordCountTableName)
    }

  /**
    * Deletes test table after each test.
    */
  private def deleteScratchTable() {
    val f = fixture
    val tableName = TableName.valueOf(WordCountTableName)
    val admin = f.connection.getAdmin()
    try {
      admin.deleteTable(tableName)
    } finally {
      admin.close()
    }
  }

  /**
    * Deletes scratch table after each test.
    */
  override def afterEach() {
    deleteScratchTable()
  }

  override def afterAll(): Unit = {
    fixture.connection.close()
  }

  "main runner" should "count words in sample file" in {
    val f = fixture
    val path = getClass.getResource("/countme.txt").getPath
    val OutputTableName = "wordcount-output"

    WordCount.runner(ProjectId, BigTableInstance,
      WordCountTableName,
      path, this.sc)

    val table = f.connection.getTable(f.tableName)
    val scanner = table.getScanner(new Scan())

    var count = 0
    var rs = scanner.next
    while ( {
      rs != null
    }) {
      count += 1
      rs = scanner.next
    }
    count should equal(139)
  }
}
