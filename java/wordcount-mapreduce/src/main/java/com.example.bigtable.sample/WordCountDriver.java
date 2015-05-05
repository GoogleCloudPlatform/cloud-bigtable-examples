/**
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.example.bigtable.sample;

import org.apache.hadoop.hbase.mapreduce.Export;
import org.apache.hadoop.util.ProgramDriver;

public class WordCountDriver {

  public static void main(String[] args) {
    ProgramDriver programDriver = new ProgramDriver();
    int exitCode = -1;
    try {
      programDriver.addClass("wordcount-hbase", WordCountHBase.class,
          "A map/reduce program that counts the words in the input files.");
      programDriver.addClass("export-table", Export.class,
          "A map/reduce program that exports a table to a file.");
      //programDriver.addClass("cellcounter", CellCounter.class, "Count them cells!");
      programDriver.driver(args);
      exitCode = programDriver.run(args);
    } catch (Throwable e) {
      e.printStackTrace();
    }
    System.exit(exitCode);
  }
}
