/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.bigtable.simpleperf;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;

import com.google.common.base.Stopwatch;

public class BigtableUtilities {
  public static final byte[] FAMILY = "cf".getBytes();
  public static final byte[] QUALIFIER = "qual".getBytes();

  /**
   * Creates a test table with a column family "cf".
   * @param tableName
   * @param conn
   * @throws IOException
   */
  public static void createTable(TableName tableName, Connection conn) throws IOException {
    System.out.println("Checking table");
    try (Admin admin = conn.getAdmin()) {
      if (!admin.tableExists(tableName)) {
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        tableDescriptor.addFamily(new HColumnDescriptor(FAMILY));
        admin.createTable(tableDescriptor);
      }
    }
  }

  public static void printPerformance(String prefix, Stopwatch stopwatch, int count) {
    long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    if (elapsed == 0) {
      System.out.println(String.format("%s %d in %d ms.", prefix, count, elapsed));
    } else {
      System.out.println(String.format("%s %d in %d ms. That's %d QPS.", prefix, count, elapsed,
        count * 1000 / elapsed));
    }
  }

}
