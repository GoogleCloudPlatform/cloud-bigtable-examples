/*
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

package com.google.cloud.examples.coinflow.sources;

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableScanConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.Coder;
import com.google.cloud.dataflow.sdk.coders.StringUtf8Coder;
import com.google.cloud.dataflow.sdk.io.Read;
import com.google.cloud.dataflow.sdk.io.UnboundedSource;
import com.google.cloud.dataflow.sdk.options.PipelineOptions;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.examples.coinflow.data.CoinbaseData;
import com.google.cloud.examples.coinflow.data.Schema;
import com.google.cloud.examples.coinflow.utils.DateHelpers;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

/**
 * This is a spout that reads from the Coinbase websocket feed, and emits
 * it as a Storm spout to the Storm stream.
 */
public class CoinbaseSource extends UnboundedSource<String, UnboundedSource.CheckpointMark> {

  private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSource.class);

  @Override
  public List<? extends UnboundedSource<String, CheckpointMark>> generateInitialSplits(
      int i, PipelineOptions pipelineOptions) throws Exception {
    return Arrays.asList(this);
  }

  @Override
  public UnboundedReader<String> createReader(
      PipelineOptions pipelineOptions, CheckpointMark checkpointMark) {
    return new CoinbaseSocket(this);
  }

  @Nullable
  @Override
  public Coder<CheckpointMark> getCheckpointMarkCoder() {
    return null;
  }

  @Override
  public void validate() {}

  @Override
  public Coder<String> getDefaultOutputCoder() {
    return StringUtf8Coder.of();
  }

  public static void main(String[] args) {
    CloudBigtableOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(CloudBigtableOptions.class);

    CloudBigtableScanConfiguration config =
        new CloudBigtableScanConfiguration.Builder()
            .withProjectId(options.getBigtableProjectId())
            .withInstanceId(options.getBigtableInstanceId())
            .withTableId(options.getBigtableTableId())
            .build();

    options.setStreaming(true);
    options.setRunner(DataflowPipelineRunner.class);

    Pipeline p = Pipeline.create(options);
    CloudBigtableIO.initializeForWrite(p);

    p.apply(Read.from(new CoinbaseSource()))
        .apply(ParDo.named("DeserializeCoinbase").of(new DeserializeCoinbase()))
        .apply(ParDo.of(new HBaseBigtableWriter()))
        .apply(CloudBigtableIO.writeToTable(config));

    p.run();
  }

  private static ObjectMapper objectMapper = initializeObjectMapper();

  private static ObjectMapper initializeObjectMapper() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper;
  }

  public static class DeserializeCoinbase extends DoFn<String, CoinbaseData> {

    @Override
    public void processElement(ProcessContext c) {
      CoinbaseData data = deserializeData(c.element());
      c.output(data);
    }

    CoinbaseData deserializeData(String str) {
      CoinbaseData data = null;
      try {
        data = objectMapper.readValue(str, CoinbaseData.class);
      } catch (IOException e) {
        LOG.warn("Failed to deserialize Coinbase message into JSON " + str, e);
      }
      return data;
    }
  }

  /**
   * Writes the element in the context to Bigtable.
   *
   */
  public static class HBaseBigtableWriter extends DoFn<CoinbaseData, Mutation> {

    @Override
    public void startBundle(DoFn<CoinbaseData, Mutation>.Context c) throws Exception {
      super.startBundle(c);
    }

    @Override
    public void processElement(DoFn<CoinbaseData, Mutation>.ProcessContext c) {
      CoinbaseData data = c.element();
      String ts = Long.toString(DateHelpers.convertDateToTime(data.getTime()));
      String rowKey = data.getType() + "_" + ts;
      String dataStr;
      try {
        dataStr = objectMapper.writeValueAsString(data);
      } catch (JsonGenerationException | JsonMappingException e) {
        LOG.error("Error serializing Coinbase data into string", e);
        return;
      } catch (IOException e) {
        LOG.error("IO exception serializing Coinbase data into string", e);
        return;
      }

      Put put = new Put(Bytes.toBytes(rowKey));
      put.addColumn(
          Bytes.toBytes(Schema.CF), Bytes.toBytes(Schema.QUALIFIER), Bytes.toBytes(dataStr));
      c.output(put);
    }

    @Override
    public void finishBundle(DoFn<CoinbaseData, Mutation>.Context c) throws Exception {
      super.finishBundle(c);
    }
  }
}
