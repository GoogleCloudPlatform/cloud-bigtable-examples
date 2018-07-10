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

// [START bigtable_connecting_helper]

package com.example.cloud.bigtable.connecting;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import com.google.cloud.bigtable.hbase.BigtableOptionsFactory;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Connection;

public class BigtableHelper {

  public static String projectId;
  public static String instanceId;
  public static String appProfileId;

  public static void main(String... args) {
    projectId = args[0];  // my-gcp-project-id
    instanceId = args[1]; // my-bigtable-instance-id

    // Include the following line if you are using app profiles.
    // If you do not include the following line, the connection uses the
    // default app profile.
    appProfileId = args[2];    // my-bigtable-app-profile-id
  }

  public static Connection connection = null;

  public static void connect() throws IOException {
    Configuration config = BigtableConfiguration.configure(projectId, instanceId);
    // Include the following line if you are using app profiles.
    // If you do not include the following line, the connection uses the
    // default app profile.
    config.set(BigtableOptionsFactory.APP_PROFILE_ID_KEY, appProfileId);

    connection = BigtableConfiguration.connect(config);
  }
}

// [END bigtable_connecting_helper]
