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
package com.google.cloud.bigtable.example.bulk_delete.simple.steps;

import com.google.cloud.bigtable.beam.AbstractCloudBigtableTableDoFn;
import com.google.cloud.bigtable.beam.CloudBigtableConfiguration;
import com.google.cloud.bigtable.util.RowKeyUtil;
import java.io.IOException;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;

/**
 * Expand a row prefix into a range and then queries Bigtable for all of the keys that
 * li in that range.
 */
public class ScanPrefixDoFn extends AbstractCloudBigtableTableDoFn<String, byte[]> {
  private final String tableId;

  public ScanPrefixDoFn(CloudBigtableConfiguration config, String tableId) {
    super(config);
    this.tableId = tableId;
  }

  @ProcessElement
  public void processElement(ProcessContext c) throws IOException {
    String prefix = c.element();
    byte[] prefixBytes = prefix.getBytes();

    byte[] start = prefixBytes;
    byte[] end = RowKeyUtil.calculateTheClosestNextRowKeyForPrefix(start);

    Scan scan = new Scan(start, end)
        .setFilter(new KeyOnlyFilter());

    Table table = getConnection().getTable(TableName.valueOf(tableId));

    for (Result result : table.getScanner(scan)) {
      c.output(result.getRow());
    }
  }
}
