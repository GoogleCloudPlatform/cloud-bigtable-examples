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
package com.google.bigtable.sample;

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
