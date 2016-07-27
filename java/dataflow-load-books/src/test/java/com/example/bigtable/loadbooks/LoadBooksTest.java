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

import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.testing.DataflowAssert;
import com.google.cloud.dataflow.sdk.testing.TestPipeline;
import com.google.cloud.dataflow.sdk.transforms.Create;
import com.google.cloud.dataflow.sdk.transforms.DoFnTester;
import com.google.cloud.dataflow.sdk.values.KV;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Tests for the {@link LoadBooks} Dataflow pipeline. */
@RunWith(JUnit4.class)
public class LoadBooksTest {

  private String testFile;

  @Before
  public void setUp() throws Exception {
    URL url = Resources.getResource("biarcs.tsv");
    testFile = Resources.toString(url, Charsets.UTF_8);
  }

  @Test
  public void parseBooks_returnsNgramsCounts() {
    // Arrange
    Pipeline p = TestPipeline.create();
    PCollection<String> input = p.apply(Create.of(testFile));

    // Act
    PCollection<KV<String, Integer>> output = LoadBooks.applyPipelineToParseBooks(input);

    // Assert
    DataflowAssert.that(output)
        .containsInAnyOrder(
            KV.of("despatch when art", 10),
            KV.of("despatch when came", 10),
            KV.of("despatch when published", 12),
            KV.of("despatch where was", 10),
            KV.of("despatch which made", 45),
            // There are two entries for "despatch which addressed".
            // Each entry has a different part of speech for "addressed".
            KV.of("despatch which addressed", 12 + 46),
            KV.of("despatch which admitted", 13),
            KV.of("despatch which allow", 14),
            KV.of("despatch which announced", 50),
            KV.of("despatch which answer", 32));
  }

  @Test
  public void doMutation_encodesKeysAndCounts() {
    // Arrange
    DoFnTester<KV<String, Integer>, Mutation> tester = DoFnTester.of(LoadBooks.ENCODE_NGRAM);
    KV<String, Integer> input = KV.of("this is a test", 513);

    // Act
    List<Mutation> output = tester.processBatch(input);

    // Assert
    Put put = (Put) output.get(0);
    assertThat(put.getRow()).isEqualTo("this is a test".getBytes(StandardCharsets.UTF_8));
    Cell valueCell = put.get(LoadBooks.FAMILY, LoadBooks.COUNT_QUALIFIER).get(0);
    byte[] valueArray = valueCell.getValueArray();
    byte[] value =
        Arrays.copyOfRange(
            valueArray,
            valueCell.getValueOffset(),
            valueCell.getValueOffset() + valueCell.getValueLength());
    assertThat(value).isEqualTo(new byte[] {0, 0, 2, 1});
  }
}
