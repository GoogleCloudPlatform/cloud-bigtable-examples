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

import com.google.cloud.bigtable.example.bulk_delete.advanced.RowKey;
import com.google.common.collect.Range;
import java.util.List;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;

/**
 * Split a scan range by region by using a side input of a {@link RowKey} distribution from
 * {@link ListRegionsDoFn}. Then assign each split into a group.
 */
public class AssignRegionDoFn extends DoFn<Range<RowKey>, KV<Integer, Range<RowKey>>> {
  private final PCollectionView<List<Range<RowKey>>> regionStartsView;

  public AssignRegionDoFn(PCollectionView<List<Range<RowKey>>> regionStarts) {
    this.regionStartsView = regionStarts;
  }

  @ProcessElement
  public void processElement(ProcessContext c) {
    List<Range<RowKey>> regions = c.sideInput(regionStartsView);

    // This can be more efficient by doing a binary search of the regions rather then checking each
    // one individually
    for(int i=0; i<regions.size(); i++) {
      Range<RowKey> region = regions.get(i);

      if (c.element().isConnected(region)) {
        Range<RowKey> overlap = c.element().intersection(region);
        if (!overlap.isEmpty()) {
          c.output(KV.of(i, overlap));
        }
      }
    }
  }
}
