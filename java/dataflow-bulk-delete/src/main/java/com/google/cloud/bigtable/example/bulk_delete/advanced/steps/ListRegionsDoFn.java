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

import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.cloud.bigtable.example.bulk_delete.advanced.RowKey;
import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.common.collect.Range;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;

/**
 * To get efficient parallelization, we want to distribute the deletes across the different regions.
 * This DoFn will fetch the regions for a given {@link CloudBigtableTableConfiguration} and return
 * them as a list of guava's Range objects. This list can in turn be used a side input for scan and
 * delete.
 */
public class ListRegionsDoFn extends DoFn<CloudBigtableTableConfiguration, List<Range<RowKey>>> {
  @ProcessElement
  public void processElement(ProcessContext c) throws IOException {
    CloudBigtableTableConfiguration tableConfig = c.element();

    try (Connection connection = BigtableConfiguration.connect(tableConfig.toHBaseConfig())) {
      TableName tableName = TableName.valueOf(tableConfig.getTableId());
      List<HRegionInfo> regions = connection.getAdmin().getTableRegions(tableName);
      List<Range<RowKey>> ranges = regions.stream()
          .map(ListRegionsDoFn::regionToRange)
          .sorted(RANGE_COMPARATOR)
          .collect(Collectors.toList());
      c.output(ranges);
    }
  }

  private static Range<RowKey> regionToRange(HRegionInfo region) {
    boolean isStartUnbounded = Arrays.equals(HConstants.EMPTY_START_ROW, region.getStartKey());
    boolean isEndUnbounded = Arrays.equals(HConstants.EMPTY_END_ROW, region.getEndKey());

    if (isStartUnbounded && isEndUnbounded) {
      return Range.all();
    } else if(isStartUnbounded) {
      return Range.lessThan(new RowKey(region.getEndKey()));
    } else if (isEndUnbounded) {
      return Range.atLeast(new RowKey(region.getStartKey()));
    } else {
      return Range.openClosed(new RowKey(region.getStartKey()), new RowKey(region.getEndKey()));
    }
  }

  private static Comparator<Range<RowKey>> RANGE_COMPARATOR = (o1, o2) -> {
    if (!o1.hasLowerBound() && !o2.hasLowerBound()) return 0;
    if (!o1.hasLowerBound()) return -1;
    if (!o2.hasLowerBound()) return 1;

    return o1.lowerEndpoint().compareTo(o2.lowerEndpoint());
  };
}
