/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.bigtable.simpleperf;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import java.util.TreeSet;

import io.grpc.StatusException;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

/**
 * Writes a specified number of rows with specified sized values to Cloud Bigtable.
 */
public class WritePerfTest {

  private static final int PRINT_COUNT = 10_000;

  public static void main(String[] args) throws IOException, InterruptedException {
    Preconditions.checkArgument(args.length == 5,
      "Usage: [project id] [instance id] [table] [row count] [value size]");
    writeTestData(args[0], args[1], TableName.valueOf(args[2]), Long.valueOf(args[3]), Integer.valueOf(args[4]));
  }

  public static void writeTestData(String projectId, String instanceId,
      TableName tableName, long rowCount, int valueSize) throws IOException {
    System.out.println("Writing to table: " + tableName);
    try (Connection conn = BigtableConfiguration.connect(projectId, instanceId)) {
      BigtableUtilities.createTable(tableName, conn);
      runMutationTests(conn, tableName, rowCount, valueSize);
    }
    System.out.println("Closed the connection");
  }

  protected static void runMutationTests(Connection conn, TableName tableName, long rowCount,
      int valueSize) throws IOException {
    System.out.println("starting mutations");
    Stopwatch uberStopwatch = Stopwatch.createUnstarted();
    Stopwatch incrementalStopwatch = Stopwatch.createUnstarted();
    try (BufferedMutator mutator = conn.getBufferedMutator(tableName)) {
      // Use the same value over and over again. Creating new random data takes time. Don't count
      // creating a large array towards Bigtable performance
      byte[] value = Bytes.toBytes(RandomStringUtils.randomAlphanumeric(valueSize));
      incrementalStopwatch.start();
      for (long i = 1; i < 10; i++) {
        // The first few writes are slow.
        doPut(mutator, value);
      }
      mutator.flush();
      BigtableUtilities.printPerformance("starter batch", incrementalStopwatch, 10);

      uberStopwatch.reset();
      incrementalStopwatch.reset();
      uberStopwatch.start();
      incrementalStopwatch.start();
      for (int i = 0; i < rowCount - 10; i++) {
        doPut(mutator, value);
        if (i > 0 && i % PRINT_COUNT == 0) {
          BigtableUtilities.printPerformance("one batch", incrementalStopwatch, PRINT_COUNT);
          BigtableUtilities.printPerformance("average so far", uberStopwatch, i);
          incrementalStopwatch.reset();
          incrementalStopwatch.start();
        }
      }
      incrementalStopwatch.reset();
      incrementalStopwatch.start();
      System.out.println("Flushing");
      mutator.flush();
      System.out.println(String.format("Flush took %d ms.",
              incrementalStopwatch.elapsed(TimeUnit.MILLISECONDS)));
      BigtableUtilities.printPerformance("full batch", uberStopwatch, PRINT_COUNT);
    } catch (RetriesExhaustedWithDetailsException e) {
      logExceptions(e);
    }
  }


  protected static void doPut(BufferedMutator mutator, byte[] value) throws IOException {
    byte[] key = Bytes.toBytes(RandomStringUtils.randomAlphanumeric(10));
    mutator.mutate(new Put(key, System.currentTimeMillis()).addColumn(BigtableUtilities.FAMILY,
    BigtableUtilities.QUALIFIER, value));
  }

  private static void logExceptions(RetriesExhaustedWithDetailsException e) {
    System.out.println(e.getExhaustiveDescription());
    Set<String> codes = new TreeSet<>();
    Set<String> messages = new TreeSet<>();
    for (Throwable e1 : e.getCauses()) {
      if (e1 instanceof StatusException) {
        StatusException statusException = (StatusException) e1;
        codes.add(statusException.getStatus().getCode().name());
        messages.add(statusException.getMessage());
      }
    }
  }
}
