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
package com.google.cloud.bigtable.example.bulk_delete.advanced;

import com.google.common.collect.Range;
import java.io.Serializable;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Simple wrapper around row key bytes. This allows us to use guava's {@link Range} class.
 */
public class RowKey implements Comparable<RowKey>, Serializable {
  private final byte[] bytes;

  public RowKey(byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public int compareTo(RowKey rowKey) {
    return Bytes.compareTo(bytes, rowKey.bytes);
  }

  public byte[] getBytes() {
    return bytes;
  }
}
