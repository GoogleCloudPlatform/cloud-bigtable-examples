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
import java.util.Collection;
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
        CommandLine line = null;
        try {
            // parse the command line arguments
            line = parser.parse( options, args );
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

        Command command = null;
        List<String> argsList = Arrays.asList(args);
        if (argsList.size() > 0) {
            command = commands.get(argsList.get(0));
        }

        if (line.hasOption("help")) {
            if (command == null) {
                help(commands.values());
            } else {
                help(command);
            }
            System.exit(0);
        } else if (command == null) {
            usage();
            System.exit(0);
        }


        try {
            Connection connection  = ConnectionFactory.createConnection();
            
            try {
                try {
                    command.run(connection, argsList.subList(1, argsList.size())); 
                } catch (InvalidArgsException e) {
                    System.out.println("ERROR: Invalid arguments");
                    usage(command);
                    System.exit(0);
                }
            } finally {
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void usage() {
        System.out.println("Usage: hbasecli.sh COMMAND [OPTIONS ...]");
        System.out.println("Try hbasecli.sh -help for more details.");
    }

    public static void usage(Command command) {
        System.out.println("Usage: ./hbasecli.sh " + command.getName() + " " + command.getOptions());
    }

    public static void help(Collection<Command> commands) {
        usage();
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("");
        for (Command command : commands) {
            System.out.println("    " + command.getName() + ": " + command.getDescription());
        }
        System.out.println("");
        System.out.println("Try hbasecli.sh COMMAND -help for command usage.");
    }

    public static void help(Command command) {
        usage(command);
        System.out.println(command.getDescription());
    }

    protected static class InvalidArgsException extends Exception {
        private List<String> args;

        public InvalidArgsException(List<String> args) {
           this.args = args;
        }

        public List<String> getArgs() {
            return this.args;
        }
    }

    protected static abstract class Command {
        private String name;

        public Command(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public abstract void run(Connection connection, List<String> args) throws InvalidArgsException, IOException;
        public abstract String getOptions();
        public abstract String getDescription();
    }

    protected static class CreateCommand extends Command {

        public CreateCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() != 1) {
                throw new InvalidArgsException(args);
            }
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

        public String getOptions() {
            return "TABLENAME [COLUMNFAMILY ...]";
        }

        public String getDescription() {
            return "Create a new table";
        }
    }

    protected static class ScanCommand extends Command {

        public ScanCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() < 1 || args.size() > 2) {
                throw new InvalidArgsException(args);
            }
            String tableName = args.get(0);
            String filterVal = null;
            if (args.size() > 1) {
                filterVal = args.get(1);
            }

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

        public String getOptions() {
            return "TABLENAME [FILTER]";
        }

        public String getDescription() {
            return "Scan a table";
        }
    }

    protected static class GetCommand extends Command {

        public GetCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() != 2) {
                throw new InvalidArgsException(args);
            }

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

        public String getOptions() {
            return "TABLENAME ROWID";
        }

        public String getDescription() {
            return "Get all columns from a single row";
        }
    }

    protected static class PutCommand extends Command {

        public PutCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() != 5) {
                throw new InvalidArgsException(args);
            }

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

        public String getOptions() {
            return "TABLENAME ROWID COLUMNFAMILY COLUMN VALUE";
        }

        public String getDescription() {
            return "Put a column value";
        }
    }

    protected static class ListCommand extends Command {

        public ListCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            String pattern = null;
            if (args.size() == 1) {
                pattern = args.get(0);
            } else if (args.size() != 0) {
                throw new InvalidArgsException(args);
            }

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

        public String getOptions() {
            return "[TABLENAME]";
        }

        public String getDescription() {
            return "List tables";
        }
    }

}
