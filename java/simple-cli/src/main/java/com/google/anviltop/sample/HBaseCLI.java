package com.google.anviltop.sample;

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

public class HBaseCLI {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: cli.sh <command> <options>");
            System.exit(1);
        }
        try {
            Connection connection  = ConnectionFactory.createConnection();
            try {
                if ("list".equals(args[0])) {
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
