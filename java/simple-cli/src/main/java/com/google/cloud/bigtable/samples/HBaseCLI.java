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
package com.google.cloud.bigtable.samples;

import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Table;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;

import org.apache.hadoop.hbase.util.Bytes;

import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.HColumnDescriptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;

public class HBaseCLI {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: cli.sh <command> <options>");
            System.exit(1);
        }
        try {
            Connection connection  = ConnectionFactory.createConnection();
            try {
                if ("create".equals(args[0])) {
                    ArrayList<String> columnFamilies = new ArrayList<String>();
                    for (int i=2; i < args.length; i++) {
                        columnFamilies.add(args[i]);
                    }
                    create(connection, args[1], columnFamilies);
                } else if ("list".equals(args[0])) {
                    if (args.length > 1) {
                        list(connection, args[1]);
                    } else {
                        list(connection, null);
                    }
                } else if ("put".equals(args[0])) {
                    put(connection, args[1], args[2], args[3], args[4], args[5]);
                } else if ("get".equals(args[0])) {
                    get(connection, args[1], args[2]);
                } else if ("scan".equals(args[0])) {
                    if (args.length > 2) {
                        scan(connection, args[1], args[2]);
                    } else {
                        scan(connection, args[1], null);
                    }
                } else {
                    System.out.println("Unknown command: " + args[0]);
                }
            } finally {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void create(Connection connection, String tableName, ArrayList<String> columnFamilies) throws IOException {
        Admin admin = connection.getAdmin();
        HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
        for (String colFamily : columnFamilies) {
            tableDescriptor.addFamily(new HColumnDescriptor(colFamily));
        }
        admin.createTable(tableDescriptor);
    }

    public static void scan(Connection connection, String tableName, String filterVal) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Scan scan = new Scan();
        if (filterVal != null) {
            String splitVal = "=";
            CompareFilter.CompareOp op = CompareFilter.CompareOp.EQUAL;

            if (filterVal.contains(">=")) {
                 op = CompareFilter.CompareOp.GREATER_OR_EQUAL;
                 splitVal = ">=";
            } else if (filterVal.contains("<=")) {
                 op = CompareFilter.CompareOp.LESS_OR_EQUAL;
                 splitVal = "<=";
            } else if (filterVal.contains(">")) {
                 op = CompareFilter.CompareOp.GREATER;
                 splitVal = ">";
            } else if (filterVal.contains("<")) {
                 op = CompareFilter.CompareOp.LESS;
                 splitVal = "<";
            }
            String[] filter = filterVal.split(splitVal);
            String[] filterCol = filter[0].split(":");
            System.out.println(filterVal);
            System.out.printf("Filter: %s:%s %s %s\n", filterCol[0], filterCol[1], op, filter[1]);
            scan.setFilter(new SingleColumnValueFilter(filterCol[0].getBytes(), filterCol[1].getBytes(), op, filter[1].getBytes()));
        }
        ResultScanner resultScanner = table.getScanner(scan);
        for (Result result : resultScanner) {
            for (Cell cell : result.listCells()) {
                String row = new String(CellUtil.cloneRow(cell));
                String family = new String(CellUtil.cloneFamily(cell));
                String column = new String(CellUtil.cloneQualifier(cell));
                String value = new String(CellUtil.cloneValue(cell));
                long timestamp = cell.getTimestamp();
                System.out.printf("%-20s column=%s:%s, timestamp=%s, value=%s\n", row, family, column, timestamp, value);
            }
        }
    }

    public static void get(Connection connection, String tableName, String rowId) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Result result = table.get(new Get(rowId.getBytes()));
        for (Cell cell : result.listCells()) {
            String row = new String(CellUtil.cloneRow(cell));
            String family = new String(CellUtil.cloneFamily(cell));
            String column = new String(CellUtil.cloneQualifier(cell));
            String value = new String(CellUtil.cloneValue(cell));
            long timestamp = cell.getTimestamp();
            System.out.printf("%-20s column=%s:%s, timestamp=%s, value=%s\n", row, family, column, timestamp, value);
        }
    }

    public static void put(Connection connection, String tableName, String rowId,
                    String columnFamily, String column, String value) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Put put = new Put(Bytes.toBytes(rowId));
        put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
        table.put(put);
    }

    public static void list(Connection connection, String pattern) throws IOException {
        Admin admin = connection.getAdmin();
        HTableDescriptor[] tables;
        if (pattern != null) {
            tables = admin.listTables(pattern);
        } else {
            tables = admin.listTables();
        }
        for (HTableDescriptor table : tables) {
            HColumnDescriptor[] columnFamilies = table.getColumnFamilies();
            String columnFamilyNames = "";
            for (HColumnDescriptor columnFamily : columnFamilies) {
                columnFamilyNames += columnFamily.getNameAsString() + ",";
            }
            if (columnFamilyNames.length() > 0) {
                columnFamilyNames = " <" + columnFamilyNames.substring(0, columnFamilyNames.length()) + ">";
            }

            System.out.println(table.getTableName() + columnFamilyNames);
        }
    }

}
