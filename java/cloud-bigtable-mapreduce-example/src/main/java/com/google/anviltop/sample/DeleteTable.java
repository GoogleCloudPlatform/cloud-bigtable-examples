/**
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
package com.google.anviltop.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 * <p>Deletes a set of tables. Usage:
 * <pre>
 * java -cp ~/cloud-bigtable-mapreduce-example-0.1.0-SNAPSHOT.jar:$(hbase classpath) $BIGTABLE_BOOT_OPTS com.google.anviltop.sample.DeleteTable [tablenames]
 * </pre>
 * @author sduskis
 */
public class DeleteTable {

  final static Log LOG = LogFactory.getLog(DeleteTable.class);

  public static void main(String[] args) throws Exception {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length < 1) {
      System.err.println("Usage: <table-name> [other table names]");
      System.exit(2);
    }

    try (Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();) {
      for (String name : otherArgs) {
        TableName tableName = TableName.valueOf(name);
        try {
          if (!admin.tableExists(tableName)) {
            LOG.info("Table " + tableName + " does not exists");
          } else {
            // NOTE: Anviltop createTable is synchronous while HBASE creation is not.
            admin.deleteTable(tableName);
            LOG.info("Table " + tableName + " deleted.");
          }
        } catch (Exception e) {
          LOG.error("Could not delete table " + name, e);
        }
      }
    }
  }
}
