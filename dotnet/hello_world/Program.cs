using Google.Cloud.Bigtable.Admin.V2;
using Google.Cloud.Bigtable.V2;
using Microsoft.Extensions.Configuration;
using System.Collections.Generic;
using System.Threading.Tasks;
using Google.Protobuf;
using System.Linq;
using System.IO;
using System;


namespace HelloWorld
{
    class Program
    {
        // Connect to bigtable and perform basic operations
        // 1. Create table with column family
        // 2. Insert rows
        // 3. Read rows
        // 4. Delete table
        //String[] x = Environment.GetCommandLineArgs();

        static HelloWorldSettings _settings;

        static void Main(string[] args)
        {
            #region Setting up application settigns
            var builder = new ConfigurationBuilder().
                SetBasePath(Directory.GetCurrentDirectory())
                .AddJsonFile("appsettings.json", optional: true, reloadOnChange: true);

            IConfigurationRoot configuration = builder.Build();

            _settings = new HelloWorldSettings();
            configuration.Bind(_settings);
            #endregion

            HelloWorld helloWorld = new HelloWorld(_settings.ProjectId, _settings.InstanceId);
            helloWorld.DoHelloWorld();
            Console.WriteLine("Press any key to exit");
            Console.ReadKey();
        }

        public class HelloWorld
        {

            public string ProjectId { get; set; }
            public string InstanceId { get; set; }

            public HelloWorld(string projectId, string instanceId)
            {
                ProjectId = projectId;
                InstanceId = instanceId;
            }

            public void DoHelloWorld()
            {

                // Build Instance Name
                InstanceName instanceName = new InstanceName(ProjectId, InstanceId);

                // Build table name Admin
                Google.Cloud.Bigtable.Admin.V2.TableName _tableAdmin = new Google.Cloud.Bigtable.Admin.V2.TableName(ProjectId, InstanceId, _settings.TableName);

                // Build table name Client
                Google.Cloud.Bigtable.V2.TableName _tableClient = new Google.Cloud.Bigtable.V2.TableName(ProjectId, InstanceId, _settings.TableName);

                #region 1. Create table with column family
                // Use try-with-resources to make sure it gets closed
                try
                {
                    BigtableTableAdminClient bigtableTableAdminClient = BigtableTableAdminClient.Create();

                    Console.WriteLine($"Creating new table: {_settings.TableName}");
                    // Checking if a table with given tableName already exists
                    var tables = bigtableTableAdminClient.ListTables(instanceName);

                    bool isTableExist = tables.Any(x => x.TableName.TableId == "HelloBigTable");

                    if (!isTableExist)
                    {
                        bigtableTableAdminClient.CreateTable(
                            instanceName,
                            _settings.TableName,
                            new Table
                            {
                                Granularity = Table.Types.TimestampGranularity.Millis,
                                ColumnFamilies =
                                {
                                    { _settings.ColumnFamily, new ColumnFamily { GcRule = new GcRule { MaxNumVersions = 1 } } }
                                }
                            });

                        Console.WriteLine($"Table {_settings.TableName} created succsessfully\n");
                    }
                    else
                    {
                        Console.WriteLine($"Table {_settings.TableName} already exist");
                    }

                    #endregion

                    #region Insert multiple rows in bulk using MutateRows

                    /* Each row has a unique row key.
                                       
                       Note: This example uses sequential numeric IDs for simplicity, but
                       this can result in poor performance in a production application.
                       Since rows are stored in sorted order by key, sequential keys can
                       result in poor distribution of operations across nodes.
                     
                       For more information about how to design a Bigtable schema for the
                       best performance, see the documentation:
                      
                       https://cloud.google.com/bigtable/docs/schema-design */

                    Console.WriteLine($"Writing some greetings to the table {_settings.TableName}");

                    //string[] greetings = new string[] { "Hello World!", "Hellow Bigtable!", "Hellow C#!" };

                    BigtableClient bigtableClient = BigtableClient.Create();

                    //building a MutateRowsRequest (contains table name and collection of entries)
                    MutateRowsRequest request = new MutateRowsRequest
                    {
                        TableNameAsTableName = _tableClient
                    };

                    for (int i = 0; i < _settings.Greetings.Length; i++)
                    {
                        string rowKey = String.Format("{0}{1}", _settings.RowKeyPrefix, i);

                        //Building an entry for every greeting (contains of row key and collection of mutations)
                        MutateRowsRequest.Types.Entry _entry = new Google.Cloud.Bigtable.V2.MutateRowsRequest.Types.Entry();
                        _entry.Mutations.Add(Mutations.SetCell(_settings.ColumnFamily, _settings.ColumnName, new BigtableByteString(_settings.Greetings[i])));
                        _entry.RowKey = ByteString.CopyFromUtf8(rowKey);

                        //adding entry to request
                        request.Entries.Add(_entry);
                    }

                    BigtableClient.MutateRowsStream response = bigtableClient.MutateRows(request);

                    Task write = CheckWrittenAsync();
                    write.Wait();

                    async Task CheckWrittenAsync()
                    {
                        IAsyncEnumerator<MutateRowsResponse> responseStream = response.ResponseStream;
                        while (await responseStream.MoveNext())
                        {
                            MutateRowsResponse current = responseStream.Current;
                        }
                    }
                    #endregion

                    #region reading the first row
                    Console.WriteLine("Reading the first row");
                    int number = 1;
                    for (int i = 0; i < number; i++)
                    {
                        BigtableByteString rowKey = String.Format("{0}{1}", _settings.RowKeyPrefix, i);
                        Row row = bigtableClient.ReadRow(_tableClient, rowKey);

                        Console.WriteLine($"\tRow key: {row.Key.ToStringUtf8()}, Value: {row.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");
                    }
                    #endregion

                    #region reading all rows

                    BigtableClient.ReadRowsStream responseRead = bigtableClient.ReadRows(new ReadRowsRequest
                    {
                        TableNameAsTableName = new Google.Cloud.Bigtable.V2.TableName(ProjectId, InstanceId, _settings.TableName)
                    });

                    Task printRead = PrintReadRowsAsync();
                    printRead.Wait();

                    async Task PrintReadRowsAsync()
                    {
                        Console.WriteLine("Reading all rows using streaming");
                        await responseRead.AsAsyncEnumerable().ForEachAsync(row =>
                        {
                            Console.WriteLine($"\tRow key: {row.Key.ToStringUtf8()}, Value: {row.Families[0].Columns[0].Cells[0].Value.ToStringUtf8()}");
                        });
                    }

                    #endregion

                    #region Deleting table
                    Console.WriteLine($"Deleting table: {_settings.TableName}");

                    bigtableTableAdminClient.DeleteTable(new Google.Cloud.Bigtable.Admin.V2.TableName(
                        ProjectId,
                        InstanceId,
                        _settings.TableName), null);

                    Console.WriteLine($"Table {_settings.TableName} deleted succsessfully");
                    #endregion

                }

                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            }
        }
    }
}