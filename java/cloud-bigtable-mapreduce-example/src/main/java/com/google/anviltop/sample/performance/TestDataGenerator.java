/**
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
package com.google.anviltop.sample.performance;

import static org.apache.hadoop.hbase.util.Bytes.toBytes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;


public class TestDataGenerator {
  private static final int ROW_KEY_SIZE = 24;
  private static final int BYTE_ARRAY_SIZE = 100;
  private static final String COL_PREFIX = "column";
  private static final byte[] CF = toBytes(SimpleBench.COLUMN_FAMILY);

  private static final Random RAND = new Random();

  private static final DataType[] DATA_TYPES = DataType.values();
  private static final int NUM_DATA_TYPES = DATA_TYPES.length;

  // public static List<Mutation> mutationList(int numColumns, Monitor putSizeMonitor, int size) {
  public static List<Mutation> mutationList(int numColumns, int size) {
    List<Mutation> result = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      result.add(mutationSupplier(numColumns));
    }
    return result;
  }

  public static List<Put> putList(int numColumns, int size) {
    List<Put> result = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      result.add(mutationSupplier(numColumns));
    }
    return result;
  }

  // private static Mutation mutationSupplier(int numColumns, Monitor putSizeMonitor) {
  private static Put mutationSupplier(int numColumns) {
    byte[] rowKey = new byte[ROW_KEY_SIZE];
    RAND.nextBytes(rowKey);
    Put put = new Put(rowKey);
    put.setDurability(Durability.ASYNC_WAL);

    for (int j = 0; j < numColumns; j++) {
      byte[] columnName = toBytes(COL_PREFIX + j);
      DataType dataType = DATA_TYPES[j % NUM_DATA_TYPES];
      byte[] columnValue = generateValueOfType(dataType);
      put.addColumn(CF, columnName, columnValue);
    }

    return put;
  }

  public static byte[] generateValueOfType(DataType dataType) {
    switch (dataType) {
    case LONG:
      return toBytes(RAND.nextLong());
    case BIG_DECIMAL:
      return toBytes(BigDecimal.valueOf(RAND.nextDouble() * RAND.nextLong()));
    case BOOLEAN:
      return toBytes(RAND.nextBoolean());
    case BYTE_ARRAY:
      byte[] bytes = new byte[BYTE_ARRAY_SIZE];
      RAND.nextBytes(bytes);
      return bytes;
    case DOUBLE:
      return toBytes(RAND.nextDouble() * RAND.nextInt());
    case FLOAT:
      return toBytes(RAND.nextFloat() * RAND.nextInt());
    case INTEGER:
      return toBytes(RAND.nextInt());
    default:
      return toBytes(RandomStringUtils.random(50));
    }
  }

  public enum DataType {
    LONG, BIG_DECIMAL, BOOLEAN, BYTE_ARRAY, DOUBLE, FLOAT, INTEGER, STRING
  }
}
