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
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;

/**
 * Creates a {@link Delete} mutation for each key.
 */
public class KeyToDeleteMutationDoFn extends DoFn<RowKey, Mutation> {
  @ProcessElement
  public void processElement(ProcessContext c) {
    byte[] key = c.element().getBytes();
    c.output(new Delete(key));
  }
}
