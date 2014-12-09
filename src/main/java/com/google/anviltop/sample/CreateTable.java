package com.google.anviltop.sample;

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
 *
 * @author sduskis
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
    Connection connection = null;
    Admin admin = null;

    try {
      connection = ConnectionFactory.createConnection(conf);
      admin = connection.getAdmin();
      // TODO: re-add this once admin.isTableAvailable() is implemented in anviltop
      // if (!admin.isTableAvailable(tableName)) {
      HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
      for (String columnFamily : columnFamilies) {
        tableDescriptor.addFamily(new HColumnDescriptor(columnFamily));
      }
      
      // NOTE: Anviltop createTable is synchronous while HBASE creation is not.
      admin.createTable(tableDescriptor);
    } catch (Exception e) {
      LOG.error("Could not create table " + tableName, e);
    } finally {
      // TODO: remove the try/catch once admin.close() doesn't throw exceptions anymore.
      try {
        if (admin != null) {
          admin.close();
        }
      } catch (Exception e) {
        LOG.error("Admin.close() threw an exception.", e);
      }
      if (connection != null) {
        connection.close();
      }
    }
  }
}
