// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

using Google.Cloud.Bigtable.Admin.V2;
using Google.Cloud.Bigtable.V2;
using Microsoft.Extensions.Configuration;
using System.Collections.Generic;
using System.Threading.Tasks;
using System.Linq;
using System.IO;
using System;


namespace HelloWorld
{
    class Program
    {
        static void Main()
        {
            #region Set up application settings.
            var builder = new ConfigurationBuilder().
                SetBasePath(Directory.GetCurrentDirectory())
                .AddJsonFile("appsettings.json", true, true);

            IConfigurationRoot configuration = builder.Build();

            HelloWorldSettings settings = new HelloWorldSettings();
            configuration.Bind(settings);
            #endregion

            HelloWorld helloWorld = new HelloWorld(settings);
            helloWorld.DoHelloWorld();
        }

        internal class HelloWorld
        {
            private readonly HelloWorldSettings _settings;

            /// <summary>
            /// Instance of <see cref="Google.Cloud.Bigtable.Admin.V2.TableName"/> used in <see cref="BigtableTableAdminClient"/> operations.
            /// </summary>
            private static Google.Cloud.Bigtable.Admin.V2.TableName _tableNameAdmin;

            /// <summary>
            /// instance of <see cref="Google.Cloud.Bigtable.V2.TableName"/> used in <see cref="BigtableClient"/> operations.
            /// </summary>
            private static Google.Cloud.Bigtable.V2.TableName _tableNameClient;

            private static BigtableTableAdminClient _bigtableTableAdminClient;

            private static BigtableClient _bigtableClient;

            /// <summary>
            /// Index of a greeting in _settings.Greetings.
            /// </summary>
            private static int _greetingIndex;

            /// <summary>
            /// This List containce mapping indices from <see cref="MutateRowsRequest"/> to _settings.Greetings.
            /// </summary>
            private List<int> _mapToOriginalGreetingIndex;

            internal HelloWorld(HelloWorldSettings settings)
            {
                _settings = settings;
                _bigtableTableAdminClient = BigtableTableAdminClient.Create();
                _bigtableClient = BigtableClient.Create();
                _tableNameAdmin = new Google.Cloud.Bigtable.Admin.V2.TableName(_settings.ProjectId, _settings.InstanceId, _settings.TableName);
                _tableNameClient = new Google.Cloud.Bigtable.V2.TableName(_settings.ProjectId, _settings.InstanceId, _settings.TableName);
            }

            internal void DoHelloWorld()
            {
                #region 1. Create table with column family.

                try
                {
                    Console.WriteLine($"Create new table: {_settings.TableName} with column family {_settings.ColumnFamily}, Instance: {_settings.InstanceId}");

                    // Check whether a table with given TableName already exists.
                    if (!DoesTableExist(_bigtableTableAdminClient, _tableNameAdmin))
                    {
                        _bigtableTableAdminClient.CreateTable(
                            new InstanceName(_settings.ProjectId, _settings.InstanceId),
                            _settings.TableName,
                            new Table
                            {
                                Granularity = Table.Types.TimestampGranularity.Millis,
                                ColumnFamilies =
                                {
                                    {
                                        _settings.ColumnFamily, new ColumnFamily
                                        {
                                            GcRule = new GcRule
                                            {
                                                MaxNumVersions = 1
                                            }
                                        }
                                    }
                                }
                            });
                        Console.WriteLine(DoesTableExist(_bigtableTableAdminClient, _tableNameAdmin)
                            ? $"Table {_settings.TableName} created succsessfully\n"
                            : $"There was a problem creating a table {_settings.TableName}");
                    }
                    else
                    {
                        Console.WriteLine($"Table: {_settings.TableName} already exist");
                    }

                    #endregion

                    #region 2. Insert multiple rows.

                    /* Each row has a unique row key.
                                       
                       Note: This example uses sequential numeric IDs for simplicity, but
                       this can result in poor performance in a production application.
                       Since rows are stored in sorted order by key, sequential keys can
                       result in poor distribution of operations across nodes.
                     
                       For more information about how to design a Bigtable schema for the
                       best performance, see the documentation:
                      
                       https://cloud.google.com/bigtable/docs/schema-design */

                    Console.WriteLine($"Write some greetings to the table {_settings.TableName}");

                    // Insert 1 row using MutateRow()
                    _greetingIndex = 0;
                    try
                    {
                        _bigtableClient.MutateRow(_tableNameClient, _settings.RowKeyPrefix + _greetingIndex, MutationBuilder(_greetingIndex));
                        Console.WriteLine($"\tGreeting: --{_settings.Greetings[_greetingIndex]}-- written successfully");

                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"\tFailed to write Greeting: --{_settings.Greetings[_greetingIndex]}");
                        Console.WriteLine(ex.Message);
                        throw;
                    }

                    // Insert multiple rows using MutateRows()
                    // Build a MutateRowsRequest (contains table name and a collection of entries).
                    MutateRowsRequest request = new MutateRowsRequest
                    {
                        TableNameAsTableName = _tableNameClient
                    };

                    _mapToOriginalGreetingIndex = new List<int>();

                    while (++_greetingIndex < _settings.Greetings.Length)
                    {
                        _mapToOriginalGreetingIndex.Add(_greetingIndex);
                        // Build an entry for every greeting (contains of row key and a collection of mutations).
                        request.Entries.Add(Mutations.CreateEntry(_settings.RowKeyPrefix + _greetingIndex,
                            MutationBuilder(_greetingIndex)));
                    }

                    BigtableClient.MutateRowsStream response = _bigtableClient.MutateRows(request);

                    Task write = CheckWrittenAsync(response);
                    write.Wait();

                    #endregion

                    #region Read the first row

                    Console.WriteLine("Read the first row");

                    int rowIndex = 0;

                    Row rowRead = _bigtableClient.ReadRow(_tableNameClient, _settings.RowKeyPrefix + rowIndex);
                    Console.WriteLine(
                        $"\tRow key: {rowRead.Key.ToStringUtf8()}, Value: {rowRead.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");

                    #endregion

                    #region Read all rows.

                    BigtableClient.ReadRowsStream responseRead = _bigtableClient.ReadRows(_tableNameClient);

                    Task printRead = PrintReadRowsAsync(responseRead);
                    printRead.Wait();

                    #endregion

                    #region Delete table.

                    Console.WriteLine($"Delete table: {_settings.TableName}");

                    _bigtableTableAdminClient.DeleteTable(_tableNameAdmin);
                    if (DoesTableExist(_bigtableTableAdminClient, _tableNameAdmin) == false)
                    {
                        Console.WriteLine($"Table: {_settings.TableName} deleted succsessfully");
                    }

                    #endregion

                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            }

            /// <summary>
            /// Checks if table with a specific <see cref="Google.Cloud.Bigtable.V2.TableName"/> is already exist on the given <see cref="InstanceName"/>
            /// </summary>
            /// <param name="bigtableTableAdminClient"> an instance of <see cref="BigtableTableAdminClient"/></param>
            /// <param name="tableName"> an instance of <see cref="Google.Cloud.Bigtable.Admin.V2.TableName"/> with tableId of the table in question</param>
            private bool DoesTableExist(BigtableTableAdminClient bigtableTableAdminClient, Google.Cloud.Bigtable.Admin.V2.TableName tableName)
            {
                try
                {
                    bigtableTableAdminClient.GetTable(tableName);
                    return true;
                }
                catch (Exception ex)
                {
                    if (ex.Message.Contains("Table not found"))
                    {
                        return false;
                    }
                    throw;
                }
            }

            /// <summary>
            /// Waits for complete <see cref="BigtableClient.MutateRowsStream"/> and checks <see cref="Google.Rpc.Status.Code"/> of every entry of the <see cref="MutateRowsRequest"/>
            /// </summary>
            /// <param name="response"> a <see cref="BigtableClient.MutateRowsStream"/></param>
            private async Task CheckWrittenAsync(BigtableClient.MutateRowsStream response)
            {
                IAsyncEnumerator<MutateRowsResponse> responseStream = response.ResponseStream;
                while (await responseStream.MoveNext())
                {
                    MutateRowsResponse current = responseStream.Current;

                    // Check whether rows where written successfully
                    foreach (var entry in current.Entries)
                    {
                        _greetingIndex = _mapToOriginalGreetingIndex[(int) entry.Index];
                        if (entry.Status.Code == 0)
                        {
                            Console.WriteLine($"\tGreeting: --{_settings.Greetings[_greetingIndex]}-- written successfully");
                        }
                        else
                        {
                            Console.WriteLine($"\tFailed to write Greeting: --{_settings.Greetings[_greetingIndex]}");
                            Console.WriteLine(entry.Status.Message);
                        }
                    }
                }
            }

            /// <summary>
            /// Builds a <see cref="Mutation"/> for <see cref="MutateRowRequest"/> or an <see cref="MutateRowsRequest.Types.Entry"/>
            /// </summary>
            /// <param name="greetingNumber"> an index of a greeting</param>
            private Mutation MutationBuilder(int greetingNumber) => 
                Mutations.SetCell(_settings.ColumnFamily, $"field{greetingNumber}", _settings.Greetings[greetingNumber]);
            
            private async Task PrintReadRowsAsync(BigtableClient.ReadRowsStream responseRead)
            {
                Console.WriteLine("Read all rows using streaming");
                await responseRead.AsAsyncEnumerable().ForEachAsync(row =>
                {
                    Console.WriteLine($"\tRow key: {row.Key.ToStringUtf8()}, Value: {row.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");
                });
            }
        }
    }
}