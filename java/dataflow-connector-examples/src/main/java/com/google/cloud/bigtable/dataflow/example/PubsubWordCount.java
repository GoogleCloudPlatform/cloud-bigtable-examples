/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.dataflow.example;

import java.io.IOException;
import java.util.Arrays;

import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.Duration;
import org.joda.time.Instant;

import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.cloud.bigtable.dataflow.CloudBigtableIO;
import com.google.cloud.bigtable.dataflow.CloudBigtableOptions;
import com.google.cloud.bigtable.dataflow.CloudBigtableTableConfiguration;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.io.TextIO;
import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.runners.DataflowPipelineRunner;
import com.google.cloud.dataflow.sdk.transforms.Count;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.IntraBundleParallelization;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.transforms.windowing.FixedWindows;
import com.google.cloud.dataflow.sdk.transforms.windowing.Window;
import com.google.cloud.dataflow.sdk.util.Transport;
import com.google.cloud.dataflow.sdk.values.KV;

/**
 * <p>
 * This is an example of reading from Cloud Pubsub and writing to Cloud Bigtable. The main method
 *  starts two jobs: one publishes messages to Cloud Pubsub, and the other one pulls messages,
 *  performs a word count for each message, and writes word count result to CBT.
 * </p>
 * This pipeline needs to be configured with four command line options for bigtable as well as
 *  pubsub:
 * </p>
 * <ul>
 * <li>--bigtableProjectId=[bigtable project]
 * <li>--bigtableInstanceId=[bigtable instance id]
 * <li>--bigtableTableId=[bigtable tableName]
 * <li>--inputFile=[file path on GCS]
 * <li>--pubsubTopic=projects/[project name]/topics/[topic name]
 * <p>
 * This example cannot be run locally using DirectPipelineRunner because PubsubIO won't work.
 * <p>
 * To run this starter example using managed resource in Google Cloud Platform, you should also
 * specify the following command-line options: --project=<YOUR_PROJECT_ID>
 * --stagingLocation=<STAGING_LOCATION_IN_CLOUD_STORAGE> --runner=BlockingDataflowPipelineRunner In
 * Eclipse, you can just modify the existing 'SERVICE' run configuration. The managed resource does
 * not require the GOOGLE_APPLICATION_CREDENTIALS, since the pipeline will use the security
 * configuration of the project specified by --project.
 */
public class PubsubWordCount {
  private static final byte[] FAMILY = Bytes.toBytes("cf");
  private static final byte[] QUALIFIER = Bytes.toBytes("qualifier");
  static final int WINDOW_SIZE = 1; // Default window duration in minutes
  private static final int INJECTORNUMWORKERS = 1; //number of workers used for injecting
                                                    //pubsub messages

  static final DoFn<KV<String, Long>, Mutation> MUTATION_TRANSFORM =
      new DoFn<KV<String, Long>, Mutation>() {
        private static final long serialVersionUID = 1L;

        @Override
        public void processElement(DoFn<KV<String, Long>, Mutation>.ProcessContext c)
            throws Exception {
          KV<String, Long> element = c.element();
          byte[] key = element.getKey().getBytes();
          byte[] count = Bytes.toBytes(element.getValue());
          c.output(new Put(key).addColumn(FAMILY, QUALIFIER, count));
        }
      };

  /**
   * Extracts words from a line and append the line's timestamp to each word, so that we can use a
   * Put instead of Increment for each word when we write them to CBT. This information will have to
   * be processed later to get a complete word count across time. The idea here is that Puts are
   * idempotent, so if a Dataflow job fails midway and is restarted, you still get accurate results,
   * even if the Put was sent two times. To get a complete word count, you'd have to perform a
   * prefix scan for the word + "|" and sum the count across the various rows.
   */
  static class ExtractWordsFn extends DoFn<String, String> {
    private static final long serialVersionUID = 0;

    @Override
    public void processElement(ProcessContext c) {
      Instant timestamp = c.timestamp();
      for (String word : c.element().split("[^a-zA-Z']+")) {
        if (!word.isEmpty()) {
          c.output(word + "|" + timestamp);
        }
      }
    }
  }

  public static interface BigtablePubsubOptions extends CloudBigtableOptions {
    @Default.Integer(WINDOW_SIZE)
    Integer getWindowSize();

    void setWindowSize(Integer value);

    String getPubsubTopic();

    void setPubsubTopic(String pubsubTopic);

    String getInputFile();

    void setInputFile(String location);
  }

  /**
   * <p>Creates a dataflow pipeline that creates the following chain:</p>
   * <ol>
   *   <li> Reads from a Cloud Pubsub topic
   *   <li> Window into fixed windows of 1 minute
   *   <li> Applies word count transform
   *   <li> Creates Puts from each of the word counts in the array
   *   <li> Performs a Bigtable Put on the items
   * </ol>
   *
   * @param args Arguments to use to configure the Dataflow Pipeline.  The first three are required
   *   when running via managed resource in Google Cloud Platform.  Those options should be omitted
   *   for LOCAL runs.  The next four arguments are to configure the Bigtable connection. The last
   *   two items are for Cloud Pubsub.
   *        --runner=BlockingDataflowPipelineRunner
   *        --project=[dataflow project] \\
   *        --stagingLocation=gs://[your google storage bucket] \\
   *        --bigtableProjectId=[bigtable project] \\
   *        --bigtableInstanceId=[bigtable instance id] \\
   *        --bigtableTableId=[bigtable tableName]
   *        --inputFile=[file path on GCS]
   *        --pubsubTopic=projects/[project name]/topics/[topic name]
   */

  public static void main(String[] args) throws Exception {
    // CloudBigtableOptions is one way to retrieve the options.  It's not required.
    BigtablePubsubOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BigtablePubsubOptions.class);

    // CloudBigtableTableConfiguration contains the project, zone, cluster and table to connect to.
    CloudBigtableTableConfiguration config =
        CloudBigtableTableConfiguration.fromCBTOptions(options);

    // In order to cancel the pipelines automatically,
    // DataflowPipelineRunner is forced to be used.
    // Also enables the 2 jobs to run at the same time.
    options.setRunner(DataflowPipelineRunner.class);

    options.as(DataflowPipelineOptions.class).setStreaming(true);
    Pipeline p = Pipeline.create(options);

    // This sets up serialization for Puts and Deletes so that Dataflow can potentially move them
    // through the network
    CloudBigtableIO.initializeForWrite(p);

    FixedWindows window = FixedWindows.of(Duration.standardMinutes(options.getWindowSize()));

    p
        .apply(PubsubIO.Read.topic(options.getPubsubTopic()))
        .apply(Window.<String> into(window))
        .apply(ParDo.of(new ExtractWordsFn()))
        .apply(Count.<String> perElement())
        .apply(ParDo.of(MUTATION_TRANSFORM))
        .apply(CloudBigtableIO.writeToTable(config));

    p.run();
    // Start a second job to inject messages into a Cloud Pubsub topic
    injectMessages(options);
  }

  private static void injectMessages(BigtablePubsubOptions options) {
    String inputFile = options.getInputFile();
    String topic = options.getPubsubTopic();
    DataflowPipelineOptions copiedOptions = options.cloneAs(DataflowPipelineOptions.class);
    copiedOptions.setStreaming(false);
    copiedOptions.setNumWorkers(INJECTORNUMWORKERS);
    copiedOptions.setJobName(copiedOptions.getJobName() + "-injector");
    Pipeline injectorPipeline = Pipeline.create(copiedOptions);
    injectorPipeline.apply(TextIO.Read.from(inputFile))
        .apply(ParDo.of(new FilterEmptyStringsFn()))
        .apply(IntraBundleParallelization.of(new PubsubBound(topic)).withMaxParallelism(20));
    injectorPipeline.run();
  }

  static class FilterEmptyStringsFn extends DoFn<String, String> {
    private static final long serialVersionUID = 0;

    @Override
    public void processElement(ProcessContext c) {
      if (!c.element().equals("")) {
        c.output(c.element());
      }
    }
  }

  /** A DoFn that publishes lines to Google Cloud PubSub. */
  public static class PubsubBound extends DoFn<String, Void> {
    private static final long serialVersionUID = 0;
    private final String outputTopic;
    public transient Pubsub pubsub;

    public PubsubBound(String outputTopic) {
      this.outputTopic = outputTopic;
    }

    @Override
    public void startBundle(Context context) {
      this.pubsub =
          Transport.newPubsubClient(context.getPipelineOptions().as(DataflowPipelineOptions.class))
              .build();
    }

    @Override
    public void processElement(ProcessContext c) throws IOException {
      PubsubMessage pubsubMessage = new PubsubMessage();
      pubsubMessage.encodeData(c.element().getBytes());
      PublishRequest publishRequest = new PublishRequest();
      publishRequest.setMessages(Arrays.asList(pubsubMessage));
      this.pubsub.projects().topics().publish(outputTopic, publishRequest).execute();
    }
  }
}
