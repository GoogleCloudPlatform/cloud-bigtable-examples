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

import com.google.cloud.ByteArray;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.AsyncConnection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.shaded.org.apache.directory.api.util.Strings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration Test for {@link BasicOperations} class. */
@RunWith(JUnit4.class)
public class TestBasicOperations {

  private static String projectId = System.getProperty("bigtable.projectID");
  private static String instanceId = System.getProperty("bigtable.instanceID");

  private static CompletableFuture<AsyncConnection> connection;
  private BasicOperations basicOp;

  @BeforeClass
  public static void setUpConnection() throws IOException {
    Assume.assumeTrue(Strings.isNotEmpty(projectId) && Strings.isNotEmpty(instanceId));
    connection = ConnectionFactory.createAsyncConnection();
  }

  @Before
  public void setUp() {
    basicOp = new BasicOperations();
  }

  @Test
  public void testTableActuallyCreated() throws InterruptedException, ExecutionException {
    TableName tableName = basicOp.getTableName();
    System.out.println("Table name " + tableName.getNameAsString());
    boolean actualResult = connection.thenCompose(s -> s.getAdmin().tableExists(tableName)).get();
    Truth.assertThat(actualResult).isTrue();
  }

  @Test
  public void testTableContainsFirstRow() {
    CompletableFuture<Result> future = basicOp.putAndGet();
    Result result = null;
    try {
      result = future.get();
      System.out.println("Fetched");
    } catch (InterruptedException | ExecutionException e) {
      throw new AssertionError("error while fetching row details");
    }
    // Make sure that you receive row details.
    Truth.assertThat(result).isNotNull();

    byte[] rowKey = "first-row".getBytes();
    Cell[] resCells = result.rawCells();
    Truth.assertThat(result.getRow()).isEqualTo(rowKey);
    Truth.assertThat(resCells[0].getQualifierArray()).isEqualTo("qualifier-get-1".getBytes());
    Truth.assertThat(resCells[1].getQualifierArray()).isEqualTo("qualifier-get-2".getBytes());
    Truth.assertThat(resCells[2].getQualifierArray()).isEqualTo("qualifier-get-3".getBytes());
  }

  @Test
  public void testTableForScan() {
    CompletableFuture<List<Result>> future = basicOp.putAllAndScanAll();
    List<Result> result = null;
    try {
      result = future.get();
    } catch (InterruptedException | ExecutionException e) {
      throw new AssertionError("error while fetching row details");
    }
    // Make sure that you receive row details.
    Truth.assertThat(result).isNotEmpty();

    List<String> expectedRows = ImmutableList.of("first-scan-row", "second-scan-row");
    List<String> rowKeyName =
        result.stream()
            .map(r -> ByteArray.copyFrom(r.getRow()).toStringUtf8())
            .collect(Collectors.toList());
    Truth.assertThat(rowKeyName).isEqualTo(expectedRows);
  }

  @After
  public void tearDown() {
    try {
      basicOp.deleteTable().get();
    } catch (InterruptedException | ExecutionException ex) {
      throw new CompletionException(ex);
    }
    System.out.println("Successfully table deleted");
  }

  @AfterClass
  public static void tearDownConnection() {
    if (connection != null) {
      connection.thenAccept(
          conn -> {
            try {
              conn.close();
            } catch (IOException e) {
              throw new CompletionException("Exception while closing the connection", e);
            }
          });
    }
  }
}
