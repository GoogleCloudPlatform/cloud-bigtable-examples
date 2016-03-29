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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Writes the results of WordCount to a new table
 */
public class CreateTable {

  final static Log LOG = LogFactory.getLog(CreateTable.class);

  public static void main(String[] args) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 1) {
      System.err.println("Usage: <table-name>");
      System.exit(2);
    }

    TableName tableName = TableName.valueOf(otherArgs[otherArgs.length - 1]);
    createTable(tableName, conf, Collections.singletonList("cf"));
  }

  public static void createTable(TableName tableName, Configuration conf,
      List<String> columnFamilies) throws IOException {
    LOG.info("Creating Table " + tableName);
    Connection connection = ConnectionFactory.createConnection(conf);
    Admin admin = null;
    try {
      admin = connection.getAdmin();
      if (tableExists(tableName, admin)) {
        LOG.info("Table " + tableName + " already exists");
      } else {
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        for (String columnFamily : columnFamilies) {
          tableDescriptor.addFamily(new HColumnDescriptor(columnFamily));
        }

        // NOTE: Anviltop createTable is synchronous while HBASE creation is not.
        admin.createTable(tableDescriptor);
      }
    } catch (Exception e) {
      LOG.error("Could not create table " + tableName, e);
    } finally {
      try {
        admin.close();
      } catch (Exception e) {
        LOG.error("Could not close the admin", e);
      }
      connection.close();
    }
  }

  private static boolean tableExists(TableName tableName, Admin admin)  {
    try {
      return admin.tableExists(tableName);
    } catch (Exception e) {
      LOG.error("Could not figure out if table " + tableName + " exists.", e);
      return false;
    } finally {
    }
  }
}
