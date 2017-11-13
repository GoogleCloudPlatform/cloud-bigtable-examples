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
import com.google.cloud.bigtable.util.RowKeyUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import java.util.Arrays;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.hadoop.hbase.HConstants;

/**
 * Convert a prefix in to its corresponding {@link RowKey} {@link Range}.
 */
public class PrefixToRangeDoFn extends DoFn<byte[], Range<RowKey>> {
  @ProcessElement
  public void processElement(ProcessContext c) {
    Preconditions.checkArgument(c.element().length > 0);

    byte[] prefix = c.element();

    if (Arrays.equals(HConstants.EMPTY_START_ROW, prefix)) {
      c.output(Range.all());
    } else {
      c.output(
          Range.closedOpen(
              new RowKey(prefix),
              new RowKey(RowKeyUtil.calculateTheClosestNextRowKeyForPrefix(prefix))
          )
      );
    }
  }
}
