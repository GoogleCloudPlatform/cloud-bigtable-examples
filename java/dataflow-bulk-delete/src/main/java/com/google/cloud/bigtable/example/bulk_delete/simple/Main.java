/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.example.bulk_delete.simple;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.cloud.bigtable.example.bulk_delete.simple.steps.DeleteKeyDoFn;
import com.google.cloud.bigtable.example.bulk_delete.simple.steps.ScanPrefixDoFn;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;

public class Main {
  public static void main(String[] args) {
    JobOptions jobOptions = PipelineOptionsFactory.fromArgs(args)
        .withValidation()
        .as(JobOptions.class);

    CloudBigtableTableConfiguration bigtableConfig = new CloudBigtableTableConfiguration.Builder()
        .withProjectId(jobOptions.getProject())
        .withInstanceId(jobOptions.getBigtableInstanceId())
        .withTableId(jobOptions.getBigtableTableId())
        .build();

    ArrayList<String> prefixes = Lists.newArrayList("prefix1", "prefix2", "prefix3");

    // randomize the prefixes to avoid hotspoting a region.
    Collections.shuffle(prefixes);

    Pipeline pipeline = Pipeline.create(jobOptions);

    pipeline.apply(Create.of(prefixes))
        .apply("Scan prefix", ParDo.of(new ScanPrefixDoFn(bigtableConfig, bigtableConfig.getTableId())))
        .apply("Create mutations", ParDo.of(new DeleteKeyDoFn()))
        .apply("Delete keys", CloudBigtableIO.writeToTable(bigtableConfig));

    pipeline.run().waitUntilFinish();
  }
}

