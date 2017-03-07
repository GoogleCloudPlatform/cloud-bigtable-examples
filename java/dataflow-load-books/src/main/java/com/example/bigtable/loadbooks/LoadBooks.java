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

import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableTableConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.Filter;
import com.google.cloud.dataflow.sdk.transforms.FlatMapElements;
import com.google.cloud.dataflow.sdk.transforms.MapElements;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.transforms.Sum;
import com.google.cloud.dataflow.sdk.values.KV;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.google.cloud.dataflow.sdk.values.TypeDescriptor;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A sample to load the Google Books Syntactic N-grams from Cloud Storage into Bigtable.
 *
 * <p>See http://commondatastorage.googleapis.com/books/syntactic-ngrams/index.html for more
 * information about the syntactic n-grams datasets.
 */
public class LoadBooks {
  private static final Charset STRING_ENCODING = StandardCharsets.UTF_8;

  static final byte[] FAMILY = "cf1".getBytes(StandardCharsets.UTF_8);
  static final byte[] COUNT_QUALIFIER = "count".getBytes(StandardCharsets.UTF_8);

  static DoFn<KV<String, Integer>, Mutation> ENCODE_NGRAM =
      new DoFn<KV<String, Integer>, Mutation>() {
        public void processElement(ProcessContext c) {
          KV<String, Integer> ngram = c.element();
          byte[] key = ngram.getKey().getBytes(STRING_ENCODING);
          int count = ngram.getValue();
          byte[] data = Bytes.toBytes(count);
          c.output(new Put(key).addColumn(FAMILY, COUNT_QUALIFIER, data));
        }
      };

  public static interface BigtableCsvOptions extends CloudBigtableOptions {
    String getColumnSeparator();

    void setColumnSeparator(String separator);

    String getInputFile();

    void setInputFile(String location);
  }

  /**
   * Returns the word from a {@code wordDetail}.
   *
   * <p>A word detail from the books dataset looks like {@code word/NN/part/2}. This extracts just
   * the word, ignoring the part of speech and other details.
   */
  private static String getWord(String wordDetail) {
    Iterable<String> detailParts = Splitter.on("/").split(wordDetail);
    return detailParts.iterator().next();
  }

  /**
   * Returns the key used for an {@code ngram}.
   *
   * <p>The n-gram includes data such as the part of speech, which is not desired in the key. This
   * removes everything but the words, leaving an n-gram of words separated by spaces.
   */
  private static String getKey(String ngram) {
    List<String> wordDetails = Splitter.on(" ").splitToList(ngram);
    List<String> words = wordDetails.stream().map(LoadBooks::getWord).collect(Collectors.toList());
    return Joiner.on(" ").join(words);
  }

  static PCollection<KV<String, Integer>> applyPipelineToParseBooks(PCollection<String> p) {
    return p.apply(
            FlatMapElements.via((String doc) -> Splitter.on("\n").split(doc))
                .withOutputType(new TypeDescriptor<String>() {}))
        .apply(Filter.byPredicate((String line) -> !line.isEmpty()))
        .apply(
            MapElements.via(
                (String line) -> {
                  List<String> elements = Splitter.on("\t").splitToList(line);
                  String ngram = elements.get(1);
                  String key = getKey(ngram);
                  Integer count = Integer.parseInt(elements.get(2));
                  return KV.<String, Integer>of(key, count);
                })
            .withOutputType(new TypeDescriptor<KV<String, Integer>>() {}))
        .apply(Sum.integersPerKey());
  }

  public static void main(String[] args) {
    // CloudBigtableOptions is one way to retrieve the options.  It's not required.
    // https://github.com/GoogleCloudPlatform/cloud-bigtable-examples/blob/master/java/dataflow-connector-examples/src/main/java/com/google/cloud/bigtable/dataflow/example/HelloWorldWrite.java
    BigtableCsvOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigtableCsvOptions.class);
    CloudBigtableTableConfiguration config =
        CloudBigtableTableConfiguration.fromCBTOptions(options);

    Pipeline p = Pipeline.create(options);

    CloudBigtableIO.initializeForWrite(p);

    PCollection<KV<String, Integer>> ngrams =
        applyPipelineToParseBooks(p.apply(TextIO.Read.from(options.getInputFile())));
    PCollection<Mutation> mutations = ngrams.apply(ParDo.of(ENCODE_NGRAM));
    mutations.apply(CloudBigtableIO.writeToTable(config));

    // Run the pipeline.
    p.run();
  }
}
