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
package com.google.cloud.bigtable.example.bulk_delete.advanced;

import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableScanConfiguration;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.AssignRegionDoFn;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.ExpandRegionRangesDoFn;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.KeyToDeleteMutationDoFn;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.ListRegionsDoFn;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.PrefixToRangeDoFn;
import com.google.cloud.bigtable.example.bulk_delete.advanced.steps.StringToBytesDoFn;
import com.google.cloud.bigtable.grpc.BigtableClusterUtilities;
import com.google.common.collect.Range;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.Values;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.values.PCollectionView;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private static final int MAX_NODES = 30;
  private static final int EXTRA_NODES = 5;

  public static void main(String[] args)
      throws IOException, GeneralSecurityException, InterruptedException {
    JobOptions options = PipelineOptionsFactory.fromArgs(args)
        .withValidation()
        .as(JobOptions.class);

    CloudBigtableTableConfiguration bigtableConfig = new CloudBigtableScanConfiguration.Builder()
        .withProjectId(options.getProject())
        .withInstanceId(options.getBigtableInstanceId())
        .withTableId(options.getBigtableTableId())
        .build();

    // Size up the cluster to avoid overloading regular operation
    BigtableClusterUtilities utils = BigtableClusterUtilities
        .forInstance(options.getProject(), options.getBigtableInstanceId());

    final int initialSize = utils.getClusterSize();
    final int expandedSize = Math.max(initialSize, Math.min(MAX_NODES, initialSize + EXTRA_NODES));

    if (initialSize != expandedSize) {
      LOG.info("Expanding cluster from {} to {}", initialSize, expandedSize);
      utils.setClusterSize(expandedSize);
    }

    try {
      run(options, bigtableConfig);
    } finally {
      if (initialSize != expandedSize) {
        LOG.info("Downsizing cluster from {} to {}", expandedSize, initialSize);
        utils.setClusterSize(initialSize);
      }
    }
  }

  private static void run(JobOptions options, CloudBigtableTableConfiguration bigtableConfig) {
    Pipeline pipeline = Pipeline.create(options);

    PCollectionView<List<Range<RowKey>>> regions = pipeline.apply(Create.of(bigtableConfig))
        .apply(ParDo.of(new ListRegionsDoFn()))
        .apply(View.asSingleton());

    pipeline
        .apply("Read prefixes from GCS", TextIO.read().from(options.getRegexFile()))
        .apply("Convert prefixes to bytes", ParDo.of(new StringToBytesDoFn()))
        .apply("Convert prefixes to ranges", ParDo.of(new PrefixToRangeDoFn())).setCoder(
        SerializableCoder.of(new TypeDescriptor<Range<RowKey>>() {}))
        .apply("Assign prefixes to regions", ParDo.of(new AssignRegionDoFn(regions)).withSideInputs(regions))
        .apply("Group the prefixes", GroupByKey.create())
        .apply("Extract the prefix groups", Values.create())
        .apply("Scan the prefixes", ParDo.of(new ExpandRegionRangesDoFn(bigtableConfig)))
        .apply("Create the deletes", ParDo.of(new KeyToDeleteMutationDoFn()))
        .apply("Write the deletes", CloudBigtableIO.writeToTable(bigtableConfig));

    pipeline.run().waitUntilFinish();
  }

}


