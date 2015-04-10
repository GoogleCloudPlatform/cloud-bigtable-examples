package com.google.anviltop.sample.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBench {
  private static enum TestType {
    BufferedMutator,
    ContinuousBufferedMutator,
    TablePutList,
    TableSinglePut
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBench.class);

  public static final String TABLE = "perf_test";
  public static final String COLUMN_FAMILY = "d";
  private static final int TOTAL_ROWS = 100_000;
  private static final int BATCH_SIZE = 1_000;
  private static final int NUM_COLUMNS = 50;
  private static final long BUFFER_SIZE_BYTES = 1048576L; // 1 MiB

  private static String table = TABLE;
  private static int totalRows = TOTAL_ROWS;

  private static TestType mode = TestType.BufferedMutator;

  public static void main(String[] args) {
    LOGGER.info("aslkfjlsdafjkdslfjas kfd Opening connection");
    if (args.length > 0) {
      table = args[0];
    }
    if (args.length > 1) {
      totalRows = Integer.parseInt(args[1]);
    }
    if (args.length > 2) {
      mode = TestType.valueOf(args[2]);
      if (mode == null) {
        LOGGER.error("Could not understand type: " + args[2]);
        System.exit(1);
      }
    }
    TableName tableName = TableName.valueOf(table);
    try (Connection conn = ConnectionFactory.createConnection()) {
      createTableIfNotExists(conn, tableName);
      switch(mode) {
      case BufferedMutator:
        doBufferedMutatorLoadTest(conn, tableName, true);
      case ContinuousBufferedMutator:
        doBufferedMutatorLoadTest(conn, tableName, false);
      case TablePutList:
        doTableLoadTest(conn, tableName, true);
      case TableSinglePut:
        doTableLoadTest(conn, tableName, false);
      }
    } catch (Exception e) {
      LOGGER.error("Caught exception", e);
    }
  }

  public static void createTableIfNotExists(Connection conn, TableName tableName) throws Exception {
    Admin admin = conn.getAdmin();
    if (!admin.tableExists(tableName)) {
      LOGGER.info("Creating table {}", table);
      HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
      HColumnDescriptor columnDescriptor = new HColumnDescriptor(COLUMN_FAMILY);
      tableDescriptor.addFamily(columnDescriptor);

      admin.createTable(tableDescriptor);
      LOGGER.info("Table created");
    }
  }

  private static void doBufferedMutatorLoadTest(Connection conn, TableName tableName, boolean flush)
      throws Exception {
    LOGGER.info("Doing buffered mutator batching");
    BufferedMutatorParams param = new BufferedMutatorParams(tableName);
    param.writeBufferSize(BUFFER_SIZE_BYTES);

    List<byte[]> rowKeys;
    try (BufferedMutator mutator = conn.getBufferedMutator(param)) {
      rowKeys = doWrites(mutator, flush);
    }

    try (Table tbl = conn.getTable(tableName)) {
      doExistingRowReads(tbl, rowKeys);
      doNonExistingRowReads(tbl);
    }
  }

  private static void doTableLoadTest(Connection conn, TableName tableName, boolean batch) throws Exception {
    List<byte[]> rowKeys;
    LOGGER.info("Doing table batching");

    try (Table table = conn.getTable(tableName)) {
      rowKeys = doTableWrites(table, batch);
      doExistingRowReads(table, rowKeys);
      doNonExistingRowReads(table);
    }
  }

  private static List<byte[]> doTableWrites(Table table, boolean doBatch) throws IOException {
    // Monitor writeTimeMonitor = MonitorFactory.start("writeTime");
    // Monitor putSizeMonitor = MonitorFactory.getMonitor("putSize", "bytes");

    List<byte[]> rowKeys = new ArrayList<>(totalRows);

    LOGGER.info("Starting writes");

    int rowsWritten = 0;
    long size = 0;
    long totalTime = 0;
    while (rowsWritten < totalRows) {
      List<Put> batch = TestDataGenerator.putList(NUM_COLUMNS, BATCH_SIZE);
      long batchSize = 0;
      for (Mutation m : batch) {
        rowKeys.add(m.getRow());
        batchSize += m.heapSize();
      }
      LOGGER.info("Committing batch");
      long batchStart = System.currentTimeMillis();
      if (doBatch) {
        table.put(batch);
      } else {
        for (Put put : batch) {
          table.put(put);
        }
      }
      long current = System.currentTimeMillis() - batchStart;
      totalTime += current;
      LOGGER.info("Batch committed: Time = {} ms,  Avg Size = {} bytes", current,
        batchSize / batch.size());
      rowsWritten += BATCH_SIZE;
      size += batchSize;
    }

    LOGGER.info("Write stats: \n{} ms\n{} bytes\n{} rows", totalTime, size, rowsWritten);
    return rowKeys;
  }

  private static List<byte[]> doWrites(BufferedMutator mutator, boolean flush) throws IOException {
    // Monitor writeTimeMonitor = MonitorFactory.start("writeTime");
    // Monitor putSizeMonitor = MonitorFactory.getMonitor("putSize", "bytes");

    List<byte[]> rowKeys = new ArrayList<>(totalRows);

    LOGGER.info("Starting writes");

    int rowsWritten = 0;
    long size = 0;
    long totalTime = 0;
    while (rowsWritten < totalRows) {
      List<Mutation> batch = TestDataGenerator.mutationList(NUM_COLUMNS, BATCH_SIZE);
      long batchSize = 0;
      for (Mutation m : batch) {
        rowKeys.add(m.getRow());
        batchSize += m.heapSize();
      }
      LOGGER.info("Committing batch");
      long batchStart = System.currentTimeMillis();
      mutator.mutate(batch);
      if (flush){
        mutator.flush();
      }
      long current = System.currentTimeMillis() - batchStart;
      totalTime += current;
      LOGGER.info("Batch committed: Time = {} ms,  Avg Size = {} bytes", current,
        batchSize / batch.size());
      rowsWritten += BATCH_SIZE;
      size += batchSize;
    }

    LOGGER.info("Write stats: \n{} ms\n{} bytes\n{} rows", totalTime, size, rowsWritten);
    return rowKeys;
  }

  private static void doExistingRowReads(Table table, List<byte[]> rowKeys) throws IOException {
    LOGGER.info("Read existing");
    long start = System.currentTimeMillis();
    for (byte[] rowKey : rowKeys) {
      Result r = table.get(new Get(rowKey));

      if (r.isEmpty() || (r.size() != NUM_COLUMNS)) {
        LOGGER.warn("Incorrect result for [{}]", Bytes.toStringBinary(rowKey));
      }
    }

    long total = System.currentTimeMillis() - start;
    LOGGER
        .info("Read stats: \ntotal: {} ms\navg: {} ms ", total, ((double) total) / rowKeys.size());
  }

  private static void doNonExistingRowReads(Table table) throws IOException {
    LOGGER.info("Read non existing");
    long start = System.currentTimeMillis();
    // Monitor readNonExistingMonitor = MonitorFactory.start("readNonExisting");
    for (int i = 0; i < 100; i++) {
      byte[] rowKey = TestDataGenerator.generateValueOfType(TestDataGenerator.DataType.BYTE_ARRAY);
      // readNonExistingMonitor.start();
      Result r = table.get(new Get(rowKey));
      // readNonExistingMonitor.stop();

      if (!r.isEmpty()) {
        LOGGER.warn("Unexpected result for {}", Bytes.toStringBinary(rowKey));
      }
    }
    LOGGER.info("Read non existing stats: \n{}", System.currentTimeMillis() - start);
  }
}
