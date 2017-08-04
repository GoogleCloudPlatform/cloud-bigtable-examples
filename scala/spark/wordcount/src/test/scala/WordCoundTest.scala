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


import java.nio.ByteBuffer

import org.apache.hadoop.hbase.client.{ConnectionFactory, Get}
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.util.Bytes
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

class WordCountTest extends FlatSpec with SparkSpec with Matchers {

  "writeWordCount" should "write to hbase" in {
    val tableName = TableName.valueOf("test2")
    var hbaseConfig = HBaseConfiguration.create()
    val TestWord = "test_word"
    val Count = 3

    val conn1 = ConnectionFactory.createConnection(hbaseConfig)
    val table = conn1.getTable(tableName)
    val mutator = conn1.getBufferedMutator(tableName)

    WordCount.writeWordCount(TestWord, Count, mutator)
    mutator.close()

    val get = new Get(Bytes.toBytes(TestWord))
    val result = table.get(get).getValue(WordCount.ColumnFamilyBytes, WordCount.ColumnNameBytes)
    val intResult = Bytes.toInt(result)
    intResult should equal(Count)
  }
}
