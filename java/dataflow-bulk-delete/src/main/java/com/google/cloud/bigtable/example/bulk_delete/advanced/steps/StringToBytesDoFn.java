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

import org.apache.beam.sdk.transforms.DoFn;

/**
 * Converts a String row prefix (presumable read from a TextIO), into a byte array.
 * This is only necessary because the source in this example is a TextIO, if reading from a
 * different source like a {@link org.apache.beam.sdk.io.AvroIO}, the keys can be stored as bytes
 * and the conversion is not necessary.
 */
public class StringToBytesDoFn extends DoFn<String, byte[]> {
  @ProcessElement
  public void processElement(ProcessContext c) {
    c.output(c.element().getBytes());
  }
}
