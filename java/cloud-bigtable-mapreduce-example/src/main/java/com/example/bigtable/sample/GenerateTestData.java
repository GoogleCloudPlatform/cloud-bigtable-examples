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
package com.example.bigtable.sample;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutator.ExceptionListener;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.GenericOptionsParser;

public class GenerateTestData {

  final static Log LOG = LogFactory.getLog(GenerateTestData.class);

  private static final byte[] family = Bytes.toBytes("cf");
  private static final byte[] qualifier = Bytes.toBytes("some qualifier");

  private static void
      writeData(BufferedMutator mutator, int count, int valueSize, String keySuffix)
          throws IOException {
    long start = System.currentTimeMillis();
    long lastStart = start;
    long max = 0;
    long min = Long.MAX_VALUE;
    // byte[] value = new byte[valueSize];
    boolean hasExceptions = false;
    for (int i = 1; i <= count && !hasExceptions; i++) {
      byte[] value = Bytes.toBytes(RandomStringUtils.randomAlphabetic(valueSize));
      byte[] key = Bytes.toBytes(RandomStringUtils.randomAlphabetic(10) + i + keySuffix);
      try {
        mutator.mutate(new Put(key).addColumn(family, qualifier, value));
      } catch (RetriesExhaustedWithDetailsException e) {
        e.printStackTrace();
        if (e.getCause() != null) {
          e.getCause().printStackTrace();
        }
        List<Throwable> causes = e.getCauses();
        for (Throwable cause : causes) {
          cause.printStackTrace();
        }
        hasExceptions = true;
      } catch (Exception e) {
        e.printStackTrace();
        if (e.getCause() != null) {
          e.getCause().printStackTrace();
        }
        hasExceptions = true;
      }
      if (i % 100 == 0) {
        long previousLastStart = lastStart;
        lastStart = logit(i, start, previousLastStart);
        long diff = lastStart - previousLastStart;
        max = Math.max(max, diff);
        min = Math.min(min, diff);
      }
    }
    System.out.println("flushing");
    mutator.flush();
    logit(count, start, lastStart);
    long total = System.currentTimeMillis() - start;
    LOG.info(String.format("finished %d in %d seconds .  100 count max: %d, min: %d", count,
      (int) (total / 1000), max, min));
  }

  private static long logit(int count, long start, long lastStart) {
    long now = System.currentTimeMillis();
    long millis = now - start;
    double seconds = millis / 1000l;
    LOG.info(String.format("Wrote %d rows.  Last 100: %d millis. that's %d rows per second.",
      count, (now - lastStart), (int) (count / seconds)));
    return now;
  }

  private static void testNoWrites(int count, int valueSize, String keySuffix) throws IOException {
    BufferedMutator nopBufferedMutator = new BufferedMutator() {

      @Override
      public void mutate(List<? extends Mutation> arg0) throws IOException {

      }

      @Override
      public void mutate(Mutation arg0) throws IOException {

      }

      @Override
      public long getWriteBufferSize() {
        return 0;
      }

      @Override
      public TableName getName() {
        return null;
      }

      @Override
      public Configuration getConfiguration() {
        return null;
      }

      @Override
      public void flush() throws IOException {

      }

      @Override
      public void close() throws IOException {
      }
    };
    writeData(nopBufferedMutator, count, valueSize, keySuffix);
  }

  public static void main(String[] args) throws IOException {
    Configuration conf = HBaseConfiguration.create();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 4) {
      System.err.println("Usage: <table-name> <number of rows> <size of value> <key suffix>");
      System.exit(2);
    }
    TableName tableName = TableName.valueOf(otherArgs[0]);
    int numberOfRows = Integer.valueOf(otherArgs[1]);
    int rowSize = Integer.valueOf(otherArgs[2]);
    String keySuffix = otherArgs[3];

    if ("do_no_buffered_writes".equals(otherArgs[0])) {
      testNoWrites(numberOfRows, rowSize, keySuffix);
      System.exit(0);
    }


    BufferedMutatorParams params = new BufferedMutatorParams(tableName);
    params.listener(new ExceptionListener() {

      @Override
      public void onException(RetriesExhaustedWithDetailsException exception,
          BufferedMutator mutator) throws RetriesExhaustedWithDetailsException {
        try {
          mutator.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        List<Throwable> causes = exception.getCauses();
        for (Throwable t : causes) {
          t.printStackTrace();
        }
      }
    });

    try (Connection connection = ConnectionFactory.createConnection(conf);
        Admin admin = connection.getAdmin();
        BufferedMutator mutator = connection.getBufferedMutator(params);) {
      if (admin.tableExists(tableName)) {
        admin.deleteTable(tableName);
      }
      HTableDescriptor desc = new HTableDescriptor(tableName);
      desc.addFamily(new HColumnDescriptor(family));
      admin.createTable(desc);

      writeData(mutator, numberOfRows, rowSize, keySuffix);
    }
  }
}
