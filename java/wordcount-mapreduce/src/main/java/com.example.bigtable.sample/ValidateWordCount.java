/**
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.example.bigtable.sample;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;

public class ValidateWordCount {

  @SuppressWarnings("unused")
  public static void main(String[] args) throws IOException {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length < 2) {
      System.err.println("Usage: java -cp <this jar>:<hbase classpath> "
          + ValidateWordCount.class.getName() + " <table-name> <expected count>");
      System.exit(2);
    }

    TableName tableName = TableName.valueOf(otherArgs[0]);
    int expectedCount = Integer.parseInt(otherArgs[1]);

    Scan scan = new Scan();
    scan.addFamily(Bytes.toBytes("cf"));
    scan.setStartRow(Bytes.toBytes(""));
    int count = 0;

    try (Connection conn = ConnectionFactory.createConnection(conf);
        Table table = conn.getTable(tableName);
        ResultScanner rs = table.getScanner(scan)) {
      for (Result result : rs) {
        count++;
      }
    }

    System.out.println("Count: " + count + ".  Expected: " + expectedCount);
    System.exit(count == expectedCount ? 0 : 1);
  }
}
