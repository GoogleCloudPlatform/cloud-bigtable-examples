/*
 * Copyright 2019 Google LLC. All Rights Reserved.
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
package com.example.bigtable.simpleasync;

import static com.example.bigtable.simpleasync.Utility.FAMILY_2;
import static com.example.bigtable.simpleasync.Utility.createSingleRowData;
import static com.example.bigtable.simpleasync.Utility.createTable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.AdvancedScanResultConsumer;
import org.apache.hadoop.hbase.client.AsyncConnection;
import org.apache.hadoop.hbase.client.AsyncTable;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;

/**
 * This example creates a {@link AsyncConnection} with properties provided at hbase-site.xml in the
 * classpath.
 */
public class BasicOperations {

  private final CompletableFuture<AsyncConnection> asyncConnection;
  private final TableName tableName;

  public BasicOperations() {
    // Creates a future for a AsyncConnection.
    asyncConnection = ConnectionFactory.createAsyncConnection();

    // Creates a future of TableName
    Future<TableName> tableFuture =
        asyncConnection.thenCompose(conn -> createTable(conn.getAdmin()));
    try {
      System.out.println("Blocking and waiting for tableName future to resolve");
      tableName = tableFuture.get();
      System.out.println("Table successfully created!!");
    } catch (InterruptedException | ExecutionException ex) {
      throw new RuntimeException("Error occurred while creating Table", ex);
    }
  }

  /** Getter for asyncConnection. */
  public CompletableFuture<AsyncConnection> getAsyncConntion() {
    return asyncConnection;
  }

  /** Getter for tableName. */
  public TableName getTableName() {
    return tableName;
  }

  /** Returns a future of {@link AsyncTable} for an input tableName. */
  public CompletableFuture<AsyncTable<AdvancedScanResultConsumer>> getAsyncTable() {
    return asyncConnection.thenApply(conn -> conn.getTable(tableName));
  }

  /**
   * Creates a row, then fetches present details of "first-row" using {@link AsyncTable#get(Get)}.
   */
  public CompletableFuture<Result> putAndGet() {
    // To fetch details of a row with provided row-id.
    Get get = new Get("first-row".getBytes());
    return getAsyncTable()
        .thenCompose(
            table ->
                // creates data in the provided table.
                createSingleRowData(table)
                    // fetches the Result object.
                    .thenCompose(next -> table.get(get)));
  }

  /**
   * Creates multiple row in the table, Once finished it scans with filter condition using {@link
   * AsyncTable#scanAll(Scan)}.
   */
  public CompletableFuture<List<Result>> putAllAndScanAll() {
    // To be used for scanning the table.
    Scan scan = new Scan();
    System.out.println("\n -------- Applying scan filter with \"cf-2\"  --------");
    scan.addFamily(FAMILY_2);
    // Sets filter condition to  ".*".
    return getAsyncTable()
        .thenCompose(
            table ->
                // Creates a table with multiple row-ids.
                Utility.createMultiRowData(table)
                    // Scans the table with provided filter conditions.
                    .thenCompose(next -> table.scanAll(scan)));
  }

  /** Deletes table present in bigtable. */
  public CompletableFuture<Void> deleteTable() {
    return asyncConnection.thenCompose(conn -> conn.getAdmin().deleteTable(tableName));
  }

  // necessary properties to connect with bigtable.
  private static String projectId = System.getProperty("bigtable.projectID");
  private static String instanceId = System.getProperty("bigtable.instanceID");

  public static void main(String[] args) {
    // Confirms presence of projectId and instanceId in classpath.
    if (projectId == null || instanceId == null) {
      System.out.println(
          "\nPlease provide bigtable.projectID & bigtable.instanceID in "
              + "environment variable before running this program.\n");
      throw new RuntimeException("Can not run without projectId & instanceId");
    }

    System.out.println(" -------- Started Bigtable Example -------- \n");
    BasicOperations example = new BasicOperations();

    try {
      System.out.println("\n -------- Started put and get against a single row -------- ");
      example
          .putAndGet()
          // Prints the received details with {@link Utitlity#printData(Result)}.
          .thenAccept(Utility::printData)
          // Blocks the Future<Result> to finish fetching and printing
          .get();

      System.out.println("\n -------- Started putAll and scanAll for multiple row -------- ");

      example
          .putAllAndScanAll()
          // Prints each Result present in the List.
          .thenAccept(results -> results.forEach(Utility::printData))
          // Blocks the Future<List<Result>> to finish fetching and printing
          .get();

      System.out.println("\n -------- Deleting table -------- ");
      example.deleteTable().get();

      System.out.println("\n -------- Completed Bigtable Example -------- ");
    } catch (InterruptedException | ExecutionException ex) {
      System.err.println("Exception while running Example: " + ex.getMessage());
      ex.printStackTrace();
      System.exit(1);
    } finally {
      System.exit(0);
    }
  }
}
