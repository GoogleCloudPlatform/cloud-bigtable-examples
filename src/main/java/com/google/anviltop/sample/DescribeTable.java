package com.google.anviltop.sample;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.util.GenericOptionsParser;

public class DescribeTable {

  public static void main(String[] args) throws IOException {
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
          HTableDescriptor tableDescriptor = admin.getTableDescriptor(tableName);
          System.out.println(tableDescriptor);

          HColumnDescriptor[] families = tableDescriptor.getColumnFamilies();

          System.out.println(String.format("%d families", families.length));
          for (HColumnDescriptor column : families) {
            System.out.println(column);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

}
