/**
 * Copyright 2016 Google Inc. All Rights Reserved.
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
using Google.Cloud.Bigtable.Admin.V2;
using Google.Cloud.Bigtable.V2;
using Microsoft.Extensions.Configuration;
using System.Collections.Generic;
using System.Threading.Tasks;
using Google.Protobuf;
using System.Linq;
using System.IO;
using System;


namespace HelloWorld {
    class Program {
        static HelloWorldSettings s_settings;

        static void Main(string[] args) {

            #region Set up application settings.
            var builder = new ConfigurationBuilder().
                SetBasePath(Directory.GetCurrentDirectory())
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

            IConfigurationRoot configuration = builder.Build();

            s_settings = new HelloWorldSettings();
            configuration.Bind(s_settings);
            #endregion

            HelloWorld helloWorld = new HelloWorld(s_settings.ProjectId, s_settings.InstanceId);
            helloWorld.DoHelloWorld();
        }

        public class HelloWorld {

            public string ProjectId { get; set; }
            public string InstanceId { get; set; }

            public HelloWorld(string projectId, string instanceId) {
                ProjectId = projectId;
                InstanceId = instanceId;
            }

            public void DoHelloWorld() {

                // Build Instance.
                InstanceName instance = new InstanceName(ProjectId, InstanceId);

                // Build TableName Admin.
                Google.Cloud.Bigtable.Admin.V2.TableName tableAdmin = new Google.Cloud.Bigtable.Admin.V2.TableName(ProjectId, InstanceId, s_settings.TableName);

                // Build TableName Client.
                Google.Cloud.Bigtable.V2.TableName tableClient = new Google.Cloud.Bigtable.V2.TableName(ProjectId, InstanceId, s_settings.TableName);

                #region 1. Create table with column family.
                try {
                    BigtableTableAdminClient bigtableTableAdminClient = BigtableTableAdminClient.Create();

                    Console.WriteLine($"Creating new table: {s_settings.TableName}");

                    // Check whether a table with given TableName already exists.
                    if (DoesTableExist(tableAdmin) == false) {
                        bigtableTableAdminClient.CreateTable(
                            instance,
                            s_settings.TableName,
                            new Table {
                                Granularity = Table.Types.TimestampGranularity.Millis,
                                ColumnFamilies =
                                {
                                    { s_settings.ColumnFamily, new ColumnFamily { GcRule = new GcRule { MaxNumVersions = 1 } } }
                                }
                            });
                        if (DoesTableExist(tableAdmin) == true) {
                            Console.WriteLine($"Table {s_settings.TableName} created succsessfully\n");
                        } else {
                            Console.WriteLine($"There was a problem creating a table {s_settings.TableName}");
                        }
                    } else {
                        Console.WriteLine($"Table {s_settings.TableName} already exist");
                    }
                    #endregion

                    #region 2. Insert multiple rows in bulk using MutateRows.
                    /* Each row has a unique row key.
                                       
                       Note: This example uses sequential numeric IDs for simplicity, but
                       this can result in poor performance in a production application.
                       Since rows are stored in sorted order by key, sequential keys can
                       result in poor distribution of operations across nodes.
                     
                       For more information about how to design a Bigtable schema for the
                       best performance, see the documentation:
                      
                       https://cloud.google.com/bigtable/docs/schema-design */

                    Console.WriteLine($"Writing some greetings to the table {s_settings.TableName}");

                    BigtableClient bigtableClient = BigtableClient.Create();

                    // Build a MutateRowsRequest (contains table name and a collection of entries).
                    MutateRowsRequest request = new MutateRowsRequest {
                        TableNameAsTableName = tableClient
                    };

                    for (int i = 0; i < s_settings.Greetings.Length; i++) {
                        string rowKey = String.Format("{0}{1}", s_settings.RowKeyPrefix, i);

                        // Build an entry for every greeting (contains of row key and a collection of mutations).
                        MutateRowsRequest.Types.Entry entry = new Google.Cloud.Bigtable.V2.MutateRowsRequest.Types.Entry();
                        entry.Mutations.Add(Mutations.SetCell(s_settings.ColumnFamily, s_settings.ColumnName, new BigtableByteString(s_settings.Greetings[i])));
                        entry.RowKey = ByteString.CopyFromUtf8(rowKey);

                        request.Entries.Add(entry);
                    }

                    BigtableClient.MutateRowsStream response = bigtableClient.MutateRows(request);

                    Task write = CheckWrittenAsync();
                    write.Wait();

                    async Task CheckWrittenAsync() {
                        IAsyncEnumerator<MutateRowsResponse> responseStream = response.ResponseStream;
                        while (await responseStream.MoveNext()) {
                            MutateRowsResponse current = responseStream.Current;

                            // Check whether rows where written successfully
                            foreach (var entry in current.Entries) {
                                if(entry.Status.Code == 0) {
                                    Console.WriteLine($"\tRow key: " +
                                        $"{request.Entries[(int)entry.Index].RowKey.ToStringUtf8()} written successfully");
                                } else {
                                    Console.WriteLine($"\tFailed to write Row key: " +
                                        $"{request.Entries[(int)entry.Index].RowKey.ToStringUtf8()}");
                                    Console.WriteLine(entry.Status.Message);
                                }
                            }
                        }
                    }
                    #endregion

                    #region Read the first row.
                    Console.WriteLine("Reading the first row");
                    int number = 1;
                    for (int i = 0; i < number; i++) {
                        BigtableByteString rowKey = String.Format("{0}{1}", s_settings.RowKeyPrefix, i);
                        Row row = bigtableClient.ReadRow(tableClient, rowKey);

                        Console.WriteLine($"\tRow key: {row.Key.ToStringUtf8()}, Value: {row.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");
                    }
                    #endregion

                    #region Read all rows.
                    BigtableClient.ReadRowsStream responseRead = bigtableClient.ReadRows(new ReadRowsRequest {
                        TableNameAsTableName = tableClient
                    });

                    Task printRead = PrintReadRowsAsync();
                    printRead.Wait();

                    async Task PrintReadRowsAsync() {
                        Console.WriteLine("Reading all rows using streaming");
                        await responseRead.AsAsyncEnumerable().ForEachAsync(row => {
                            Console.WriteLine($"\tRow key: {row.Key.ToStringUtf8()}, Value: {row.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");
                        });
                    }
                    #endregion

                    #region Delete table.
                    Console.WriteLine($"Deleting table: {s_settings.TableName}");

                    bigtableTableAdminClient.DeleteTable(new Google.Cloud.Bigtable.Admin.V2.TableName(
                        ProjectId,
                        InstanceId,
                        s_settings.TableName), null);
                    if (DoesTableExist(tableAdmin) == false) {
                        Console.WriteLine($"Table {s_settings.TableName} deleted succsessfully");
                    }
                    #endregion

                } catch (Exception ex) {
                    Console.WriteLine(ex.Message);
                }
            }

            public bool? DoesTableExist(Google.Cloud.Bigtable.Admin.V2.TableName tableName) {

                BigtableTableAdminClient bigtableTableAdminClient = BigtableTableAdminClient.Create();
                try {
                    var response = bigtableTableAdminClient.GetTable(tableName);
                    return true;
                } catch (Exception ex) {
                    if (ex.Message.Contains("Table not found")) {
                        return false;
                    } else {
                        return null;
                    }
                }
            }
        }
    }
}