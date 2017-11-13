/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.example.bulk_delete.advanced.steps;

import com.google.cloud.bigtable.beam.AbstractCloudBigtableTableDoFn;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.cloud.bigtable.example.bulk_delete.advanced.RowKey;
import com.google.cloud.bigtable.hbase.BigtableExtendedScan;
import com.google.cloud.bigtable.util.RowKeyUtil;
import com.google.common.collect.Range;
import java.io.IOException;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;

/**
 * Run a scan over the given a list of {@link RowKey} {@link Range}s. Outputing the keys
 * that match.
 */
public class ExpandRegionRangesDoFn extends AbstractCloudBigtableTableDoFn<Iterable<Range<RowKey>>, RowKey> {
  private final String tableId;
  private Connection connection;
  private Table table;

  public ExpandRegionRangesDoFn(CloudBigtableTableConfiguration config) {
    super(config);
    this.tableId = config.getTableId();
  }


  @Setup
  public void setup() throws IOException {
    connection = getConnection();
    table = getConnection().getTable(TableName.valueOf(tableId));
  }

  @Teardown
  public void tearDown() throws IOException {
    table.close();
    connection.close();
  }

  @ProcessElement
  public void processElement(ProcessContext c) throws IOException {
    Iterable<Range<RowKey>> prefixes = c.element();

    BigtableExtendedScan scan = new BigtableExtendedScan();
    scan.setFilter(new KeyOnlyFilter());

    for (Range<RowKey> prefix : prefixes) {
      addRange(scan, prefix);
    }

    try (ResultScanner scanner = table.getScanner(scan)) {
      for (Result result : scanner) {
        c.output(new RowKey(result.getRow()));
      }
    }
  }

  /**
   * Convert a guava range into an entry in the {@link BigtableExtendedScan}.
   */
  private void addRange(BigtableExtendedScan scan, Range<RowKey> range) {
    byte[] start = HConstants.EMPTY_START_ROW;
    byte[] end = HConstants.EMPTY_END_ROW;

    if (range.hasLowerBound()) {
      switch(range.lowerBoundType()) {
        case CLOSED:
          start = range.lowerEndpoint().getBytes();
          break;
        case OPEN:
          start = RowKeyUtil.calculateTheClosestNextRowKeyForPrefix(range.lowerEndpoint().getBytes());
          break;
      }
    }

    if (range.hasUpperBound()) {
      switch (range.upperBoundType()) {
        case CLOSED:
          end = RowKeyUtil.calculateTheClosestNextRowKeyForPrefix(range.upperEndpoint().getBytes());
          break;
        case OPEN:
          end = range.upperEndpoint().getBytes();
          break;
      }
    }

    scan.addRange(start, end);
  }

}
