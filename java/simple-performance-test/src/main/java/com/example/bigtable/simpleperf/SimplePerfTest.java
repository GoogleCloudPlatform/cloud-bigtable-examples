/*
 * Copyright 2015 Google Inc. All Rights Reserved. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package com.example.bigtable.simpleperf;

import java.io.IOException;

import org.apache.hadoop.hbase.TableName;

import com.google.common.base.Preconditions;

public class SimplePerfTest {

  public static void main(String[] args) throws NumberFormatException, IOException {
    Preconditions.checkArgument(args.length == 6,
      "Usage: [type of test: just 'local-write' for now] [project id] [Instance id] [table] [row count] [value size]");
    WritePerfTest.writeTestData(args[1], args[2], TableName.valueOf(args[3]), Long.parseLong(args[4]), Integer.parseInt(args[5]));
  }
}
