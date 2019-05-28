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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.AdvancedScanResultConsumer;
import org.apache.hadoop.hbase.client.AsyncAdmin;
import org.apache.hadoop.hbase.client.AsyncTable;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptor;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;

/** Contains utility methods to support {@link BasicOperations}. */
final class Utility {

  private Utility() {}

  public static final byte[] FAMILY_1 = "cf-1".getBytes();
  public static final byte[] FAMILY_2 = "cf-2".getBytes();
  private static final String QUALIFIER = "qualifier-1";

  /** Returns a string, to be used as table name. */
  private static String getTableName() {
    return "Test_Table_" + UUID.randomUUID().toString().substring(0, 10);
  }

  /**
   * Create asynchronously a table in Bigtable instance. First checks if a table with the same name
   * already exist. If yes, then uniquely generates another tableName with UUID.
   */
  @VisibleForTesting
  static CompletableFuture<TableName> createTable(AsyncAdmin admin) {
    final TableName table = TableName.valueOf(getTableName());

    // Creates columnFamily to be used throughout rows.
    ColumnFamilyDescriptor cf_1 =
        ColumnFamilyDescriptorBuilder.newBuilder(FAMILY_1)
            .setValue(QUALIFIER, "first-CF-value")
            .build();
    ColumnFamilyDescriptor cf_2 =
        ColumnFamilyDescriptorBuilder.newBuilder(FAMILY_2)
            .setValue(QUALIFIER, "second-CF-value")
            .build();

    // Here AsyncAdmin#tableExist returns a future.
    System.out.printf(
        "Checking if table with \"%s\" name already exist\n", table.getNameAsString());
    return admin
        .tableExists(table)
        .thenApply(
            isExist -> {
              if (isExist) {
                System.out.println("Table already existed, So creating with a new name");
                return TableName.valueOf(getTableName());
              }
              System.out.printf(
                  "No table existed with \"%s\" name, now creating one\n", table.getNameAsString());
              return table;
            })
        .thenCompose(
            tableName -> {

              // Adds ColumnFamilies to table.
              TableDescriptor tableDescriptor =
                  TableDescriptorBuilder.newBuilder(tableName)
                      .setColumnFamilies(ImmutableList.of(cf_1, cf_2))
                      .build();
              return admin.createTable(tableDescriptor).thenApply(created -> tableName);
            });
  }

  /**
   * This operation creates a single row and populate qualifier and values against columnFamily with
   * some values using {@link AsyncTable#put(Put)}.
   */
  @VisibleForTesting
  static CompletableFuture<Void> createSingleRowData(
      AsyncTable<AdvancedScanResultConsumer> asyncTable) {
    System.out.printf("Creating a single row data with row key as \"%s\" \n\n", "first-row");
    Put firstRow =
        new Put("first-row".getBytes())
            .addColumn(FAMILY_1, "qualifier-get-1".getBytes(), "first-value".getBytes())
            .addColumn(FAMILY_1, "qualifier-get-2".getBytes(), "second-value".getBytes())
            .addColumn(FAMILY_2, "qualifier-get-3".getBytes(), "third-value".getBytes());

    // After this, table would have above data in "first-row".
    return asyncTable.put(firstRow);
  }

  /**
   * This operation puts multiple rows with different qualifier and values using {@link
   * AsyncTable#putAll(List)}.
   */
  @VisibleForTesting
  static CompletableFuture<Void> createMultiRowData(
      AsyncTable<AdvancedScanResultConsumer> asyncTable) {
    System.out.println("Creating a multi row data on which Filter can be applied");

    System.out.printf("Creating row with row-id: \"%s\" \n", "first-scan-now");
    // Creates a row with "first-scan-row" as row-key with different column, qualifier and value.
    Put firstRow =
        new Put("first-scan-row".getBytes())
            .addColumn(FAMILY_2, "qualifier-scan-1".getBytes(), "first-value".getBytes())
            .addColumn(FAMILY_2, "qualifier-scan-2".getBytes(), "second-value".getBytes())
            .addColumn(FAMILY_1, "qualifier-scan-3".getBytes(), "third-value".getBytes());

    System.out.printf("Creating row with row-id: \"%s\" \n\n", "second-scan-now");
    // Creates a row with "second-scan-row" as row-key with different column, qualifier and value.
    Put secondRow =
        new Put("second-scan-row".getBytes())
            .addColumn(FAMILY_2, "qualifier-scan-one".getBytes(), "first-value".getBytes())
            .addColumn(FAMILY_1, "qualifier-scan-two".getBytes(), "second-value".getBytes())
            .addColumn(FAMILY_2, "qualifier-scan-three".getBytes(), "third value".getBytes());

    // AsyncAdmin#putAll expects List<Put>, so creating an immutable list.
    ImmutableList<Put> puts = ImmutableList.of(firstRow, secondRow);

    // After it resolves, table will have two more rows.
    return asyncTable.putAll(puts);
  }

  /** Prints data present in {@link Result} object. */
  @VisibleForTesting
  static void printData(Result result) {
    // Iterates the results. Each Cell is a value for column, So multiple Cells will be
    // processed for each row.
    for (Cell cell : result.listCells()) {
      // We use the CellUtil class to clone values from the returned cells.
      String row = new String(CellUtil.cloneRow(cell));
      String family = new String(CellUtil.cloneFamily(cell));
      String column = new String(CellUtil.cloneQualifier(cell));
      String value = new String(CellUtil.cloneValue(cell));
      long timestamp = cell.getTimestamp();
      System.out.printf(
          "%-20s column=%s:%s, timestamp=%s, value=%s\n", row, family, column, timestamp, value);
    }
  }
}
