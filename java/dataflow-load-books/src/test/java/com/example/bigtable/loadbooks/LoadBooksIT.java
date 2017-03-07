/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.bigtable.loadbooks;

import static com.google.common.truth.Truth.assertThat;

import com.google.bigtable.repackaged.com.google.cloud.hbase.BigtableConfiguration;
import com.google.common.io.Resources;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Integration tests for the {@link LoadBooks} Dataflow pipeline. */
@RunWith(JUnit4.class)
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public class LoadBooksIT {
  private static final String EMULATOR_ENV = "BIGTABLE_EMULATOR_HOST";
  private static final String INSTANCE_ENV = "BIGTABLE_INSTANCE_ID";
  private static final String PROJECT_ENV = "GOOGLE_CLOUD_PROJECT";
  private static final String TABLE_NAME = "helloworld";
  private static final byte[] TABLE_ID = "helloworld".getBytes(StandardCharsets.UTF_8);
  private static final byte[] COLUMN_FAMILY_ID = "cf1".getBytes(StandardCharsets.UTF_8);
  private static final byte[] COLUMN_ID = "count".getBytes(StandardCharsets.UTF_8);

  String testDataPath;
  String project;
  String instanceId;
  Connection bigtableConnection;

  @Before
  public void setUp() throws Exception {
    URL testDataUrl = Resources.getResource("biarcs.tsv");
    testDataPath = testDataUrl.getPath();

    String emulator = System.getenv(EMULATOR_ENV);
    boolean isEmulatorSet = emulator != null && !emulator.isEmpty();
    project = System.getenv(PROJECT_ENV);

    if (project == null || project.isEmpty()) {
      project = "ignored";
      assert isEmulatorSet
          : String.format("%s must be set if %s is not set", EMULATOR_ENV, PROJECT_ENV);
    }

    instanceId = System.getenv(INSTANCE_ENV);

    if (instanceId == null || instanceId.isEmpty()) {
      instanceId = "ignored";
      assert isEmulatorSet
          : String.format("%s must be set if %s is not set", EMULATOR_ENV, INSTANCE_ENV);
    }

    Configuration configuration = BigtableConfiguration.configure(project, instanceId);
    bigtableConnection = BigtableConfiguration.connect(configuration);
  }

  @After
  public void tearDown() throws Exception {
    bigtableConnection.close();
  }

  private int getCount(String key) throws IOException {
    Table table = bigtableConnection.getTable(TableName.valueOf(TABLE_ID));
    Result result = table.get(new Get(key.getBytes(StandardCharsets.UTF_8)));
    return ByteBuffer.wrap(result.getValue(COLUMN_FAMILY_ID, COLUMN_ID)).getInt();
  }

  @Test
  public void main_writesBigtable() throws Exception {
    LoadBooks.main(
        new String[] {
          "--project=" + project,
          "--bigtableProjectId=" + project,
          "--bigtableInstanceId=" + instanceId,
          "--bigtableTableId=" + TABLE_NAME,
          "--runner=InProcessPipelineRunner",
          "--inputFile=" + testDataPath
        });

    assertThat(getCount("despatch when art")).isEqualTo(10);
    // There are two entries for "despatch which addressed".
    // Each entry has a different part of speech for "addressed".
    assertThat(getCount("despatch which addressed")).isEqualTo(12 + 46);
  }
}
