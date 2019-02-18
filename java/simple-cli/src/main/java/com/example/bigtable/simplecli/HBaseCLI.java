/**
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/**
 * A simple command line interface to Cloud Bigtable using
 * the native HBase API. This application does not use anything
 * that is specific to Cloud Bigtable so it should work on a
 * normal HBase installation as well.
 *
 * This is not meant to be a full featured command line interface
 * or shell but to illustrate how to do some simple operations
 * on Cloud Bigtable using the HBase native API.
 */
public class HBaseCLI {

    /**
     * The main method for the CLI. This method takes the command line
     * arguments and runs the appropriate commands.
     */
    public static void main(String[] args) {
        // We use Apache commons-cli to check for a help option.
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

        // Create a list of commands that are supported. Each
        // command defines a run method and some methods for
        // printing help.
        // See the definition of each command below.
        HashMap<String, Command> commands = new HashMap<String, Command>();
        commands.put("create", new CreateCommand("create"));
        commands.put("scan", new ScanCommand("scan"));
        commands.put("get", new GetCommand("get"));
        commands.put("put", new PutCommand("put"));
        commands.put("list", new ListCommand("list"));
        commands.put("delete", new DeleteCommand("delete"));

        Command command = null;
        List<String> argsList = Arrays.asList(args);
        if (argsList.size() > 0) {
            command = commands.get(argsList.get(0));
        }

        // Check for the help option and if it's there
        // display the appropriate help.
        if (line.hasOption("help")) {
            // If there is a command listed (e.g. create -help)
            // then show the help for that command
            if (command == null) {
                help(commands.values());
            } else {
                help(command);
            }
            System.exit(0);
        } else if (command == null) {
            // No valid command was given so print the usage.
            usage();
            System.exit(0);
        }


        try {
            Connection connection  = ConnectionFactory.createConnection();

            try {
                try {
                    // Run the command with the arguments after the command name.
                    command.run(connection, argsList.subList(1, argsList.size()));
                } catch (InvalidArgsException e) {
                    System.out.println("ERROR: Invalid arguments");
                    usage(command);
                    System.exit(0);
                }
            } finally {
                // Make sure the connection is closed even if
                // an exception occurs.
                connection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Print the usage for the program.
     */
    public static void usage() {
        System.out.println("Usage: hbasecli.sh COMMAND [OPTIONS ...]");
        System.out.println("Try hbasecli.sh -help for more details.");
    }

    /**
     * Print the usage for a specific command.
     * @param command The command whose usage you want to print.
     */
    public static void usage(Command command) {
        System.out.println("Usage: ./hbasecli.sh " + command.getName() + " " + command.getOptions());
    }

    /**
     * Print the program help message. Includes a list of the supported commands.
     * @param commands A collection of the available commands.
     */
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

    /**
     * Prints the help for a specific command.
     * @param command The command whose help you want to print.
     */
    public static void help(Command command) {
        usage(command);
        System.out.println(command.getDescription());
    }

    /**
     * An exception that is thrown when invalid arguments are
     * passed to a command.
     */
    protected static class InvalidArgsException extends Exception {
        private List<String> args;

        public InvalidArgsException(List<String> args) {
           this.args = args;
        }

        public List<String> getArgs() {
            return this.args;
        }
    }

    /**
     * Defines the interface for commands that can be run by the CLI
     */
    protected static abstract class Command {
        private String name;

        public Command(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        /**
         * Run the command.
         * @param connection The HBase/Cloud Bigtable connection object.
         * @param args A list of args to the command.
         */
        public abstract void run(Connection connection, List<String> args) throws InvalidArgsException, IOException;

        /**
         * Gets a string describing command line arguments of the command.
         * Used when printing usage and help.
         */
        public abstract String getOptions();

        /**
         * Gets a string describing what the command does.
         * Used when printing usage and help.
         */
        public abstract String getDescription();
    }

    /**
     * This command creates a new Bigtable table. It uses the
     * HBase Admin class to create the table based on a
     * HTableDescriptior.
     */
    protected static class CreateCommand extends Command {

        public CreateCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() < 1) {
                throw new InvalidArgsException(args);
            }
            String tableName = args.get(0);
            ArrayList<String> columnFamilies = new ArrayList<String>();
            if (args.size() > 1) {
                for (String arg : args.subList(1, args.size())) {
                    columnFamilies.add(arg);
                }
            }

            // Create the table based on the passed in arguments.
            // We used the standard HBase Admin and HTableDescriptor classes.
            Admin admin = connection.getAdmin();
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
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

    /**
     * This command will scan a Bigtable table. Scanning,
     * by default, returns all columns from all rows in the table.
     * Many more options are available when scanning a table so
     * please see the documentation for the standard HBase Scan
     * and ResultScanner classes.
     */
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

            // Create a new Scan instance.
            Scan scan = new Scan();

            // This command supports using a columnvalue filter.
            // The filter takes the form of <columnfamily>:<column><operator><value>
            // An example would be cf:col>=10
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

    /**
     * A simple get command. This command takes the form of
     * "get [rowid]" and prints all columns for the row.
     */
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

            // Create a new Get request and specify the rowId passed by the user.
            Result result = table.get(new Get(rowId.getBytes()));

            // Iterate of the results. Each Cell is a value for column
            // so multiple Cells will be processed for each row.
            for (Cell cell : result.listCells()) {
                // We use the CellUtil class to clone values
                // from the returned cells.
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

    /**
     * This command puts a single column value to a table.
     * It takes the form of "put [table] [rowid] [columnfamily] [column] [value]
     */
    protected static class PutCommand extends Command {

        public PutCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() != 5) {
                throw new InvalidArgsException(args);
            }

            // Get the arguments passed by the user.
            String tableName = args.get(0);
            String rowId = args.get(1);
            String columnFamily = args.get(2);
            String column = args.get(3);
            String value = args.get(4);

            Table table = connection.getTable(TableName.valueOf(tableName));

            // Create a new Put request.
            Put put = new Put(Bytes.toBytes(rowId));

            // Here we add only one column value to the row but
            // multiple column values can be added to the row at
            // once by calling this method multiple times.
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));

            // Execute the put on the table.
            table.put(put);
        }

        public String getOptions() {
            return "TABLENAME ROWID COLUMNFAMILY COLUMN VALUE";
        }

        public String getDescription() {
            return "Put a column value";
        }
    }

    /**
     * This command lists the tables in your Cloud Bigtable Cluster
     * It also accepts a glob pattern argument which can be used to limit
     * the number of tables listed.
     */
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

            // We use the listTables() method on the Admin instance
            // to get a list of HTableDescriptor objects.
            if (pattern != null) {
                tables = admin.listTables(pattern);
            } else {
                tables = admin.listTables();
            }

            // For each of the tables we get the table name and column families
            // registered with the table, and print them out.
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
            return "[TABLENAME PATTERN]";
        }

        public String getDescription() {
            return "List tables";
        }
    }

    /** Delete table from cbt */
    protected static class DeleteCommand extends Command {

        public DeleteCommand(String name) {
            super(name);
        }

        public void run(Connection connection, List<String> args) throws InvalidArgsException, IOException {
            if (args.size() < 1) {
                throw new InvalidArgsException(args);
            }
            String tableId = args.get(0);

            // Deletes the table based on the passed tableId in arguments.
            // We used the standard HBase Admin and HTableDescriptor classes.
            Admin admin = connection.getAdmin();
            admin.deleteTable(TableName.valueOf(tableId));
        }

        public String getOptions() {
            return "TABLENAME";
        }

        public String getDescription() {
            return "delete an existing table";
        }
    }

}
