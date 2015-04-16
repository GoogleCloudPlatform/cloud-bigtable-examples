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
package com.example.bigtable.simplecli;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;

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
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class HBaseCLI {
    public static void main(String[] args) {
        Options options = new Options();
        Option help = new Option( "help", "print this message" );
        options.addOption(help);

        // create the parser
        CommandLineParser parser = new BasicParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println(exp.getMessage());
            usage();
            System.exit(1);
        }

        HashMap<String, Command> commands = new HashMap<String, Command>();
        commands.put("create", new CreateCommand("create"));
        commands.put("scan", new ScanCommand("scan"));
        commands.put("get", new GetCommand("get"));
        commands.put("put", new PutCommand("put"));
        commands.put("list", new ListCommand("list"));

        try {
            Connection connection  = ConnectionFactory.createConnection();
            
            try {
                Command command = commands.get(args[0]);
                List<String> argsList = Arrays.asList(args);
                command.run(connection, argsList.subList(1, argsList.size())); 
            } finally {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void usage() {
        System.out.println("Usage: hbasecli.sh <command> <options>");
    }

    protected static abstract class Command {
        private String name;

        public Command(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public abstract void run(Connection connection, List<String> args) throws IOException;
    }

    protected static class CreateCommand extends Command {

        public CreateCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws IOException {
            String tableName = args.get(0);
            ArrayList<String> columnFamilies = new ArrayList<String>();
            for (String arg : args.subList(1, args.size() - 1)) {
                columnFamilies.add(arg);
            }

            Admin admin = connection.getAdmin();
            HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
            for (String colFamily : columnFamilies) {
                tableDescriptor.addFamily(new HColumnDescriptor(colFamily));
            }
            admin.createTable(tableDescriptor);
        }
    }

    protected static class ScanCommand extends Command {

        public ScanCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws IOException {
            String tableName = args.get(0);
            String filterVal = args.get(1); // TODO 

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
    }

    protected static class GetCommand extends Command {

        public GetCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws IOException {

            String tableName = args.get(0);
            String rowId = args.get(1);

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
    }

    protected static class PutCommand extends Command {

        public PutCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws IOException {
            String tableName = args.get(0);
            String rowId = args.get(1);
            String columnFamily = args.get(2);
            String column = args.get(3);
            String value = args.get(4);

            Table table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(rowId));
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
            table.put(put);
        }
    }

    protected static class ListCommand extends Command {

        public ListCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws IOException {
            String pattern = args.get(0); // TODO        

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

}
