/**
 * Copyright 2016 Google Inc. All Rights Reserved.
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
package com.example.cloud.bigtable.helloworld;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;

/**
 * A minimal application that connects to Cloud Bigtable using the native HBase API
 * and performs some basic operations.
 */
public class HelloWorld {

  // Refer to table metadata names by byte array in the HBase API
  private static final byte[] TABLE_NAME = Bytes.toBytes("Hello-Bigtable");
  private static final byte[] COLUMN_FAMILY_NAME = Bytes.toBytes("cf1");
  private static final byte[] COLUMN_NAME = Bytes.toBytes("greeting");

  // Write some friendly greetings to Cloud Bigtable
  private static final String[] GREETINGS =
      { "Hello World!", "Hello Cloud Bigtable!", "Hello HBase!" };

  /**
   * Connects to Cloud Bigtable, runs some basic operations and prints the results
   */
  private static void doHelloWorld(String projectId, String zone, String clusterId) {

    // Create the Bigtable connection, use try-with-resources to make sure it gets closed
    try (Connection connection = BigtableConfiguration.connect(projectId, zone, clusterId)) {

      // The admin API lets us create, manage and delete tables
      Admin admin = connection.getAdmin();

      // Create a table with a single column family
      HTableDescriptor descriptor = new HTableDescriptor(TableName.valueOf(TABLE_NAME));
      descriptor.addFamily(new HColumnDescriptor(COLUMN_FAMILY_NAME));

      print("Create table " + descriptor.getNameAsString());
      admin.createTable(descriptor);

      // Retrieve the table we just created so we can do some reads and writes
      Table table = connection.getTable(TableName.valueOf(TABLE_NAME));

      // Write some rows to the table
      print("Write some greetings to the table");
      for (int i = 0; i < GREETINGS.length; i++) {
        // Each row has a unique row key
        String rowKey = "greeting" + i;

        // Put a single row into the table. We could also pass a list of Puts to write a batch.
        Put put = new Put(Bytes.toBytes(rowKey));
        put.addColumn(COLUMN_FAMILY_NAME, COLUMN_NAME, Bytes.toBytes(GREETINGS[i]));
        table.put(put);
      }

      // Get the first greeting by row key
      String rowKey = "greeting0";
      Result getResult = table.get(new Get(Bytes.toBytes(rowKey)));
      String greeting = Bytes.toString(getResult.getValue(COLUMN_FAMILY_NAME, COLUMN_NAME));
      System.out.println(
          String.format("Get a single greeting by row key: %s = %s", rowKey, greeting));

      // Now scan across all rows.
      Scan scan = new Scan();

      print("Scan for all greetings:");
      ResultScanner scanner = table.getScanner(scan);
      for (Result row : scanner) {
        byte[] valueBytes = row.getValue(COLUMN_FAMILY_NAME, COLUMN_NAME);
        System.out.println('\t' + Bytes.toString(valueBytes));
      }

      // Clean up by disabling and then deleting the table
      print("Delete the table");
      admin.disableTable(table.getName());
      admin.deleteTable(table.getName());

    } catch (IOException e) {
      System.err.println("Exception while running HelloWorld: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

    System.exit(0);
  }

  private static void print(String msg) {
    System.out.println("HelloWorld: " + msg);
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println("Usage: HelloWorld <projectId> <zone> <clusterId>");
      System.exit(1);
    }

    doHelloWorld(args[0], args[1], args[2]);
  }

}
