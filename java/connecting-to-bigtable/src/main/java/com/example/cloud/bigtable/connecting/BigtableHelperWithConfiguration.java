/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// [START bigtable_connecting_helper_with_config]
package com.example.cloud.bigtable.connecting;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

public class BigtableHelperWithConfiguration {

  public static Connection connection = null;

  public static void connect() throws IOException {
    // [START bigtable_creating_connection_object]
    Configuration config = HBaseConfiguration.create();
    connection = ConnectionFactory.createConnection(config);
    // [END bigtable_creating_connection_object]
  }
}
// [START bigtable_connecting_helper_with_config]