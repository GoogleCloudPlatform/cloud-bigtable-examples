package com.google.anviltop.sample;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HRegionLocation;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.RegionLocator;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;

public class ListRegions {

  public static void main(String[] args) throws IOException {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 1) {
      System.err.println("Usage: <table-name>");
      System.exit(2);
    }

    System.out.println("==== connecting");
    try (Connection connection = ConnectionFactory.createConnection(conf);
        RegionLocator locator = connection.getRegionLocator(TableName.valueOf(otherArgs[0]))) {
      System.out.println("==== getAllRegionLocations");
      for (HRegionLocation location : locator.getAllRegionLocations()) {
        System.out.println("==== location");
        HRegionInfo regionInfo = location.getRegionInfo();
        System.out.println(Bytes.toString(regionInfo.getStartKey()) + " - "
            + Bytes.toString(regionInfo.getEndKey()));
      }
    } catch (Throwable t) {
      System.out.println("==== exception");
      t.printStackTrace();
    }
  }
}
