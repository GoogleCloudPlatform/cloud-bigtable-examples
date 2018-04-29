/**
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.example.cloud.bigtable.createinstance;

import static com.google.cloud.bigtable.config.BigtableOptions.BIGTABLE_ADMIN_HOST_DEFAULT;
import static com.google.cloud.bigtable.config.BigtableOptions.BIGTABLE_DATA_HOST_DEFAULT;
import static com.google.cloud.bigtable.config.BigtableOptions.BIGTABLE_PORT_DEFAULT;

import static com.google.cloud.bigtable.config.BulkOptions.BIGTABLE_ASYNC_MUTATOR_COUNT_DEFAULT;
import static com.google.cloud.bigtable.config.BulkOptions.BIGTABLE_BULK_AUTOFLUSH_MS_DEFAULT;
import static com.google.cloud.bigtable.config.BulkOptions.BIGTABLE_BULK_MAX_REQUEST_SIZE_BYTES_DEFAULT;
import static com.google.cloud.bigtable.config.BulkOptions.BIGTABLE_BULK_MAX_ROW_KEY_COUNT_DEFAULT;
import static com.google.cloud.bigtable.config.BulkOptions.BIGTABLE_MAX_INFLIGHT_RPCS_PER_CHANNEL_DEFAULT;

import static com.google.cloud.bigtable.config.CallOptionsConfig.LONG_TIMEOUT_MS_DEFAULT;
import static com.google.cloud.bigtable.config.CallOptionsConfig.SHORT_TIMEOUT_MS_DEFAULT;
import static com.google.cloud.bigtable.config.CallOptionsConfig.USE_TIMEOUT_DEFAULT;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.cloud.bigtable.config.BigtableOptions;
import com.google.cloud.bigtable.config.BulkOptions;
import com.google.cloud.bigtable.config.CallOptionsConfig;
import com.google.cloud.bigtable.config.CredentialOptions;
import com.google.cloud.bigtable.config.Logger;
import com.google.cloud.bigtable.config.RetryOptions;

import com.google.common.base.Preconditions;

import io.grpc.Status;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.util.VersionInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Static methods to convert an instance of
 * {@link org.apache.hadoop.conf.Configuration} to a
 * {@link com.google.cloud.bigtable.config.BigtableOptions} instance.
 *
 * @author sduskis
 * @version $Id: $Id
 */
public class BigtableOptionsFactoryCreateInstance {
  /** Constant <code>LOG.</code> */
  protected static final Logger LOG = new Logger(BigtableOptionsFactoryCreateInstance.class);

  /** Constant <code>BIGTABLE_PORT_KEY="google.bigtable.endpoint.port"</code> */
  public static final String BIGTABLE_PORT_KEY = "google.bigtable.endpoint.port";
  /**
   * Constant
   * <code>BIGTABLE_ADMIN_HOST_KEY="google.bigtable.admin.endpoint.host"</code>
   */
  public static final String BIGTABLE_ADMIN_HOST_KEY = "google.bigtable.admin.endpoint.host";
  /** Constant <code>BIGTABLE_HOST_KEY="google.bigtable.endpoint.host"</code> */
  public static final String BIGTABLE_HOST_KEY = "google.bigtable.endpoint.host";

  /**
   * Constant
   * <code>BIGTABLE_HOST_KEY="google.bigtable.emulator.endpoint.host"</code>.
   * Values should be of format: `host:port`
   */
  public static final String BIGTABLE_EMULATOR_HOST_KEY = "google.bigtable.emulator.endpoint.host";

  /** Constant <code>PROJECT_ID_KEY="google.bigtable.project.id"</code> */
  public static final String PROJECT_ID_KEY = "google.bigtable.project.id";
  /** Constant <code>INSTANCE_ID_KEY="google.bigtable.instance.id"</code> */
  public static final String INSTANCE_ID_KEY = "google.bigtable.instance.id";
  /** Constant <code>APP_PROFILE_ID_KEY="google.bigtable.app_profile.id"</code> */
  public static final String APP_PROFILE_ID_KEY = "google.bigtable.app_profile.id";

  /**
   * Constant
   * <code>BIGTABLE_SNAPSHOT_CLUSTER_ID_KEY="google.bigtable.snapshot.cluster.id"</code>
   */
  public static final String BIGTABLE_SNAPSHOT_CLUSTER_ID_KEY = 
      "google.bigtable.snapshot.cluster.id";
  /**
   * Constant
   * <code>BIGTABLE_SNAPSHOT_DEFAULT_TTL_SECS_KEY="google.bigtable.snapshot.default.ttl.secs"</code>
   */
  public static final String BIGTABLE_SNAPSHOT_DEFAULT_TTL_SECS_KEY = 
      "google.bigtable.snapshot.default.ttl.secs";

  /**
   * Constant
   * <code>CUSTOM_USER_AGENT_KEY="google.bigtable.custom.user.agent"</code>
   */
  public static final String CUSTOM_USER_AGENT_KEY = 
      "google.bigtable.custom.user.agent";

  /**
   * Key to set to enable service accounts to be used, either metadata
   * server-based or P12-based. Defaults to enabled.
   */
  public static final String BIGTABLE_USE_SERVICE_ACCOUNTS_KEY = 
      "google.bigtable.auth.service.account.enable";
  /** Constant <code>BIGTABLE_USE_SERVICE_ACCOUNTS_DEFAULT=true.</code> */
  public static final boolean BIGTABLE_USE_SERVICE_ACCOUNTS_DEFAULT = true;

  /**
   * Key to allow unit tests to proceed with an invalid credential configuration.
   */
  public static final String BIGTABLE_NULL_CREDENTIAL_ENABLE_KEY = 
      "google.bigtable.auth.null.credential.enable";
  /** Constant <code>BIGTABLE_NULL_CREDENTIAL_ENABLE_DEFAULT=false.</code> */
  public static final boolean BIGTABLE_NULL_CREDENTIAL_ENABLE_DEFAULT = false;

  /**
   * Key to set when using P12 keyfile authentication. The value should be the
   * service account email address as displayed. If this value is not set and
   * using service accounts is enabled, a metadata server account will be used.
   */
  public static final String BIGTABLE_SERVICE_ACCOUNT_EMAIL_KEY = 
      "google.bigtable.auth.service.account.email";

  /**
   * Key to set to a location where a P12 keyfile can be found that corresponds to
   * the provided service account email address.
   */
  public static final String BIGTABLE_SERVICE_ACCOUNT_P12_KEYFILE_LOCATION_KEY = 
      "google.bigtable.auth.service.account.keyfile";

  /**
   * Key to set to a location where a json security credentials file can be found.
   */
  public static final String BIGTABLE_SERVICE_ACCOUNT_JSON_KEYFILE_LOCATION_KEY = 
      "google.bigtable.auth.json.keyfile";

  /**
   * Key to set to a json security credentials string.
   */
  public static final String BIGTABLE_SERVICE_ACCOUNT_JSON_VALUE_KEY = 
      "google.bigtable.auth.json.value";

  /**
   * Key to set to a boolean flag indicating whether or not grpc retries should be
   * enabled. The default is to enable retries on failed idempotent operations.
   */
  public static final String ENABLE_GRPC_RETRIES_KEY = "google.bigtable.grpc.retry.enable";

  /**
   * By default, the hbase client will set the timestamp on the client side before
   * sending a Put if it's not set already. That's done to ensure that only a
   * single cell is written for the put if retries occur. Retries aren't common,
   * and writing multiple cells isn't a problem in many cases. Sometimes, setting
   * the server timestamp is beneficial. If you want the server-side time stamp,
   * set this to true.
   */
  public static final String ALLOW_NO_TIMESTAMP_RETRIES_KEY = 
      "google.bigtable.alllow.no.timestamp.retries";

  /**
   * Key to set to a comma separated list of grpc codes to retry. See
   * {@link io.grpc.Status.Code} for more information.
   */
  public static final String ADDITIONAL_RETRY_CODES = "google.bigtable.grpc.retry.codes";

  /**
   * Key to set to a boolean flag indicating whether or not to retry grpc call on
   * deadline exceeded. This flag is used only when grpc retries is enabled.
   */
  public static final String ENABLE_GRPC_RETRY_DEADLINEEXCEEDED_KEY = 
      "google.bigtable.grpc.retry.deadlineexceeded.enable";

  /**
   * Key to set the initial amount of time to wait for retries, given a backoff
   * policy on errors. This flag is used only when grpc retries is enabled.
   */
  public static final String INITIAL_ELAPSED_BACKOFF_MILLIS_KEY = 
      "google.bigtable.grpc.retry.initial.elapsed.backoff.ms";

  /**
   * Key to set the maximum amount of time to wait for retries, given a backoff
   * policy on errors. This flag is used only when grpc retries is enabled.
   */
  public static final String MAX_ELAPSED_BACKOFF_MILLIS_KEY = 
      "google.bigtable.grpc.retry.max.elapsed.backoff.ms";

  /**
   * Key to set the amount of time to wait when reading a partial row.
   */
  public static final String READ_PARTIAL_ROW_TIMEOUT_MS = 
      "google.bigtable.grpc.read.partial.row.timeout.ms";

  /**
   * Key to set the number of time to retry after a scan timeout.
   */
  public static final String MAX_SCAN_TIMEOUT_RETRIES = 
      "google.bigtable.grpc.retry.max.scan.timeout.retries";

  /**
   * Key to set the maximum number of messages to buffer when scanning.
   */
  public static final String READ_BUFFER_SIZE = "google.bigtable.grpc.read.streaming.buffer.size";

  /**
   * The number of grpc channels to open for asynchronous processing such as puts.
   */
  public static final String BIGTABLE_DATA_CHANNEL_COUNT_KEY = "google.bigtable.grpc.channel.count";

  /**
   * Constant <code>BIGTABLE_USE_BULK_API="google.bigtable.use.bulk.api"</code>
   */
  public static final String BIGTABLE_USE_BULK_API = "google.bigtable.use.bulk.api";

  /**
   * Constant
   * <code>BIGTABLE_BULK_MAX_REQUEST_SIZE_BYTES=
   * "google.bigtable.bulk.max.request.size.b"{trunked}.</code>
   */
  public static final String BIGTABLE_BULK_MAX_REQUEST_SIZE_BYTES = 
      "google.bigtable.bulk.max.request.size.bytes";

  /**
   * Constant
   * <code>BIGTABLE_BULK_MAX_ROW_KEY_COUNT="google.bigtable.bulk.max.row.key.count".</code>
   */
  public static final String BIGTABLE_BULK_MAX_ROW_KEY_COUNT = 
      "google.bigtable.bulk.max.row.key.count";

  /**
   * Constant
   * <code>BIGTABLE_BULK_AUTOFLUSH_MS_KEY="google.bigtable.bulk.autoflush.ms".</code>
   */
  public static final String BIGTABLE_BULK_AUTOFLUSH_MS_KEY = 
      "google.bigtable.bulk.autoflush.ms";

  /**
   * Constant
   * <code>MAX_INFLIGHT_RPCS_KEY="google.bigtable.buffered.mutator.max.in"{trunked}.</code>
   */
  public static final String MAX_INFLIGHT_RPCS_KEY = 
      "google.bigtable.buffered.mutator.max.inflight.rpcs";

  /**
   * The maximum amount of memory to be used for asynchronous buffered mutator
   * RPCs.
   */
  public static final String BIGTABLE_BUFFERED_MUTATOR_MAX_MEMORY_KEY = 
      "google.bigtable.buffered.mutator.max.memory";

  /**
   * Turn on a feature that will reduce the likelihood of BufferedMutator
   * overloading a Cloud Bigtable cluster.
   */
  public static final String BIGTABLE_BUFFERED_MUTATOR_ENABLE_THROTTLING = 
      "google.bigtable.buffered.mutator.throttling.enable";

  /**
   * Tweak the throttling.
   */
  public static final String BIGTABLE_BUFFERED_MUTATOR_THROTTLING_THRESHOLD_MILLIS = 
      "google.bigtable.buffered.mutator.throttling.threshold.ms";

  /**
   * Constant
   * <code>BIGTABLE_USE_PLAINTEXT_NEGOTIATION="google.bigtable.use.plaintext.negotiation".</code>
   */
  public static final String BIGTABLE_USE_PLAINTEXT_NEGOTIATION = 
      "google.bigtable.use.plaintext.negotiation";

  /**
   * Constant
   * <code>BIGTABLE_USE_CACHED_DATA_CHANNEL_POOL=
   * "google.bigtable.use.cached.data.channel.pool".</code>
   */
  public static final String BIGTABLE_USE_CACHED_DATA_CHANNEL_POOL = 
      "google.bigtable.use.cached.data.channel.pool";

  /**
   * The number of asynchronous workers to use for buffered mutator operations.
   */
  public static final String BIGTABLE_ASYNC_MUTATOR_COUNT_KEY = 
      "google.bigtable.buffered.mutator.async.worker.count";

  /**
   * Should timeouts be used? Currently, this feature is experimental.
   */
  public static final String BIGTABLE_USE_TIMEOUTS_KEY = "google.bigtable.rpc.use.timeouts";

  /**
   * If timeouts are set, how many milliseconds should pass before a
   * DEADLINE_EXCEEDED? Currently, this feature is experimental.
   */
  public static final String BIGTABLE_RPC_TIMEOUT_MS_KEY = "google.bigtable.rpc.timeout.ms";

  /**
   * If timeouts are set, how many milliseconds should pass before a
   * DEADLINE_EXCEEDED for a long read? Currently, this feature is experimental.
   */
  public static final String BIGTABLE_LONG_RPC_TIMEOUT_MS_KEY = 
      "google.bigtable.long.rpc.timeout.ms";

  /**
   * Allow namespace methods to be no-ops.
   */
  public static final String BIGTABLE_NAMESAPCE_WARNING_KEY = "google.bigtable.namespace.warnings";

  /**
   * <p>
   * fromConfiguration.
   * </p>
   *
   * @param configuration
   *          a {@link org.apache.hadoop.conf.Configuration} object.
   * @return a {@link com.google.cloud.bigtable.config.BigtableOptions} object.
   * @throws java.io.IOException
   *           if any.
   */
  public static BigtableOptions fromConfiguration(final Configuration configuration)
      throws IOException {

    BigtableOptions.Builder bigtableOptionsBuilder = new BigtableOptions.Builder();
    bigtableOptionsBuilder.setProjectId(configuration.get("google.bigtable.project.id"));
    String appProfileId = configuration.get(APP_PROFILE_ID_KEY, null);
    if (appProfileId != null) {
      bigtableOptionsBuilder.setAppProfileId(appProfileId);
    }

    bigtableOptionsBuilder.setDataHost(
        getHost(configuration, BIGTABLE_HOST_KEY, BIGTABLE_DATA_HOST_DEFAULT, "API Data"));

    bigtableOptionsBuilder.setAdminHost(
        getHost(configuration, BIGTABLE_ADMIN_HOST_KEY, BIGTABLE_ADMIN_HOST_DEFAULT, "Admin"));

    int port = configuration.getInt(BIGTABLE_PORT_KEY, BIGTABLE_PORT_DEFAULT);
    bigtableOptionsBuilder.setPort(port);
    bigtableOptionsBuilder.setUsePlaintextNegotiation(
        configuration.getBoolean(BIGTABLE_USE_PLAINTEXT_NEGOTIATION, false));

    String emulatorHost = configuration.get(BIGTABLE_EMULATOR_HOST_KEY);
    if (emulatorHost != null) {
      bigtableOptionsBuilder.enableEmulator(emulatorHost);
    }

    setBulkOptions(configuration, bigtableOptionsBuilder);
    setChannelOptions(configuration, bigtableOptionsBuilder);
    setClientCallOptions(configuration, bigtableOptionsBuilder);

    return bigtableOptionsBuilder.build();
  }

  private static String getHost(Configuration configuration, String key, String defaultVal,
      String type) {
    String hostName = configuration.get(key, defaultVal);
    LOG.debug("%s endpoint host %s.", type, hostName);
    return hostName;
  }

  private static void setChannelOptions(Configuration configuration,
      BigtableOptions.Builder builder) throws IOException {
    setCredentialOptions(builder, configuration);

    builder.setRetryOptions(createRetryOptions(configuration));

    int channelCount = configuration.getInt(BIGTABLE_DATA_CHANNEL_COUNT_KEY,
        BigtableOptions.BIGTABLE_DATA_CHANNEL_COUNT_DEFAULT);
    builder.setDataChannelCount(channelCount);

    // This is primarily used by Dataflow where connections open and close often.
    // This is a
    // performance optimization that will reduce the cost to open connections.
    boolean useCachedDataPool = configuration.getBoolean(BIGTABLE_USE_CACHED_DATA_CHANNEL_POOL,
        false);
    builder.setUseCachedDataPool(useCachedDataPool);

    // This information is in addition to bigtable-client-core version, and jdk
    // version.
    StringBuilder agentBuilder = new StringBuilder();
    agentBuilder.append("hbase-").append(VersionInfo.getVersion());
    String customUserAgent = configuration.get(CUSTOM_USER_AGENT_KEY);
    if (customUserAgent != null) {
      agentBuilder.append(',').append(customUserAgent);
    }
    builder.setUserAgent(agentBuilder.toString());
  }

  private static void setBulkOptions(final Configuration configuration,
      BigtableOptions.Builder bigtableOptionsBuilder) {
    BulkOptions.Builder bulkOptionsBuilder = new BulkOptions.Builder();

    int asyncMutatorCount = configuration.getInt(BIGTABLE_ASYNC_MUTATOR_COUNT_KEY,
        BIGTABLE_ASYNC_MUTATOR_COUNT_DEFAULT);
    bulkOptionsBuilder.setAsyncMutatorWorkerCount(asyncMutatorCount);

    bulkOptionsBuilder.setUseBulkApi(configuration.getBoolean(BIGTABLE_USE_BULK_API, true));
    bulkOptionsBuilder.setBulkMaxRowKeyCount(configuration.getInt(BIGTABLE_BULK_MAX_ROW_KEY_COUNT,
        BIGTABLE_BULK_MAX_ROW_KEY_COUNT_DEFAULT));
    bulkOptionsBuilder.setBulkMaxRequestSize(configuration.getLong(
        BIGTABLE_BULK_MAX_REQUEST_SIZE_BYTES, BIGTABLE_BULK_MAX_REQUEST_SIZE_BYTES_DEFAULT));
    bulkOptionsBuilder.setAutoflushMs(
        configuration.getLong(BIGTABLE_BULK_AUTOFLUSH_MS_KEY, BIGTABLE_BULK_AUTOFLUSH_MS_DEFAULT));

    int defaultRpcCount = BIGTABLE_MAX_INFLIGHT_RPCS_PER_CHANNEL_DEFAULT
        * bigtableOptionsBuilder.getDataChannelCount();
    int maxInflightRpcs = configuration.getInt(MAX_INFLIGHT_RPCS_KEY, defaultRpcCount);
    bulkOptionsBuilder.setMaxInflightRpcs(maxInflightRpcs);

    long maxMemory = configuration.getLong(BIGTABLE_BUFFERED_MUTATOR_MAX_MEMORY_KEY,
        BulkOptions.BIGTABLE_MAX_MEMORY_DEFAULT);
    bulkOptionsBuilder.setMaxMemory(maxMemory);

    if (configuration.getBoolean(BIGTABLE_BUFFERED_MUTATOR_ENABLE_THROTTLING,
        BulkOptions.BIGTABLE_BULK_ENABLE_THROTTLE_REBALANCE_DEFAULT)) {
      bulkOptionsBuilder.enableBulkMutationThrottling();
      bulkOptionsBuilder.setBulkMutationRpcTargetMs(
          configuration.getInt(BIGTABLE_BUFFERED_MUTATOR_THROTTLING_THRESHOLD_MILLIS,
              BulkOptions.BIGTABLE_BULK_THROTTLE_TARGET_MS_DEFAULT));
    }

    bigtableOptionsBuilder.setBulkOptions(bulkOptionsBuilder.build());
  }

  private static void setCredentialOptions(BigtableOptions.Builder builder,
      Configuration configuration) throws FileNotFoundException {
    if (configuration.getBoolean(BIGTABLE_USE_SERVICE_ACCOUNTS_KEY,
        BIGTABLE_USE_SERVICE_ACCOUNTS_DEFAULT)) {
      LOG.debug("Using service accounts");

      if (configuration.get(BIGTABLE_SERVICE_ACCOUNT_JSON_VALUE_KEY) != null) {
        String jsonValue = configuration.get(BIGTABLE_SERVICE_ACCOUNT_JSON_VALUE_KEY);
        LOG.debug("Using json value");
        builder.setCredentialOptions(CredentialOptions.jsonCredentials(jsonValue));
      } else if (configuration.get(BIGTABLE_SERVICE_ACCOUNT_JSON_KEYFILE_LOCATION_KEY) != null) {
        String keyfileLocation = configuration
            .get(BIGTABLE_SERVICE_ACCOUNT_JSON_KEYFILE_LOCATION_KEY);
        LOG.debug("Using json keyfile: %s", keyfileLocation);
        builder.setCredentialOptions(
            CredentialOptions.jsonCredentials(new FileInputStream(keyfileLocation)));
      } else if (configuration.get(BIGTABLE_SERVICE_ACCOUNT_EMAIL_KEY) != null) {
        String serviceAccount = configuration.get(BIGTABLE_SERVICE_ACCOUNT_EMAIL_KEY);
        LOG.debug("Service account %s specified.", serviceAccount);
        String keyfileLocation = configuration
            .get(BIGTABLE_SERVICE_ACCOUNT_P12_KEYFILE_LOCATION_KEY);
        Preconditions.checkState(!isNullOrEmpty(keyfileLocation),
            "Key file location must be specified when setting service account email");
        LOG.debug("Using p12 keyfile: %s", keyfileLocation);
        builder
            .setCredentialOptions(CredentialOptions.p12Credential(serviceAccount, keyfileLocation));
      } else {
        LOG.debug("Using default credentials.");
        builder.setCredentialOptions(CredentialOptions.defaultCredentials());
      }
    } else if (configuration.getBoolean(BIGTABLE_NULL_CREDENTIAL_ENABLE_KEY,
        BIGTABLE_NULL_CREDENTIAL_ENABLE_DEFAULT)) {
      builder.setCredentialOptions(CredentialOptions.nullCredential());
      LOG.info("Enabling the use of null credentials. This should not be used in production.");
    } else {
      throw new IllegalStateException("Either service account or null credentials must be enabled");
    }
  }

  private static void setClientCallOptions(Configuration configuration,
      BigtableOptions.Builder bigtableOptionsBuilder) {
    CallOptionsConfig.Builder clientCallOptionsBuilder = new CallOptionsConfig.Builder();

    clientCallOptionsBuilder
        .setUseTimeout(configuration.getBoolean(BIGTABLE_USE_TIMEOUTS_KEY, USE_TIMEOUT_DEFAULT));
    clientCallOptionsBuilder.setShortRpcTimeoutMs(
        configuration.getInt(BIGTABLE_RPC_TIMEOUT_MS_KEY, SHORT_TIMEOUT_MS_DEFAULT));
    clientCallOptionsBuilder.setLongRpcTimeoutMs(
        configuration.getInt(BIGTABLE_LONG_RPC_TIMEOUT_MS_KEY, LONG_TIMEOUT_MS_DEFAULT));
    bigtableOptionsBuilder.setCallOptionsConfig(clientCallOptionsBuilder.build());
  }

  private static RetryOptions createRetryOptions(Configuration configuration) {
    RetryOptions.Builder retryOptionsBuilder = new RetryOptions.Builder();
    boolean enableRetries = configuration.getBoolean(ENABLE_GRPC_RETRIES_KEY,
        RetryOptions.DEFAULT_ENABLE_GRPC_RETRIES);
    LOG.debug("gRPC retries enabled: %s", enableRetries);
    retryOptionsBuilder.setEnableRetries(enableRetries);

    boolean allowRetriesWithoutTimestamp = configuration.getBoolean(ALLOW_NO_TIMESTAMP_RETRIES_KEY,
        false);
    LOG.debug("allow retries without timestamp: %s", enableRetries);
    retryOptionsBuilder.setAllowRetriesWithoutTimestamp(allowRetriesWithoutTimestamp);

    String retryCodes = configuration.get(ADDITIONAL_RETRY_CODES, "");
    String[] codes = retryCodes.split(",");
    for (String stringCode : codes) {
      String trimmed = stringCode.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      Status.Code code = Status.Code.valueOf(trimmed);
      Preconditions.checkArgument(code != null, "Code " + stringCode + " not found.");
      LOG.debug("gRPC retry on: %s", stringCode);
      retryOptionsBuilder.addStatusToRetryOn(code);
    }

    boolean retryOnDeadlineExceeded = configuration
        .getBoolean(ENABLE_GRPC_RETRY_DEADLINEEXCEEDED_KEY, true);
    LOG.debug("gRPC retry on deadline exceeded enabled: %s", retryOnDeadlineExceeded);
    retryOptionsBuilder.setRetryOnDeadlineExceeded(retryOnDeadlineExceeded);

    int initialElapsedBackoffMillis = configuration.getInt(INITIAL_ELAPSED_BACKOFF_MILLIS_KEY,
        RetryOptions.DEFAULT_INITIAL_BACKOFF_MILLIS);
    LOG.debug("gRPC retry initialElapsedBackoffMillis: %d", initialElapsedBackoffMillis);
    retryOptionsBuilder.setInitialBackoffMillis(initialElapsedBackoffMillis);

    int maxElapsedBackoffMillis = configuration.getInt(MAX_ELAPSED_BACKOFF_MILLIS_KEY,
        RetryOptions.DEFAULT_MAX_ELAPSED_BACKOFF_MILLIS);
    LOG.debug("gRPC retry maxElapsedBackoffMillis: %d", maxElapsedBackoffMillis);
    retryOptionsBuilder.setMaxElapsedBackoffMillis(maxElapsedBackoffMillis);

    int readPartialRowTimeoutMillis = configuration.getInt(READ_PARTIAL_ROW_TIMEOUT_MS,
        RetryOptions.DEFAULT_READ_PARTIAL_ROW_TIMEOUT_MS);
    LOG.debug("gRPC read partial row timeout (millis): %d", readPartialRowTimeoutMillis);
    retryOptionsBuilder.setReadPartialRowTimeoutMillis(readPartialRowTimeoutMillis);

    int streamingBufferSize = configuration.getInt(READ_BUFFER_SIZE,
        RetryOptions.DEFAULT_STREAMING_BUFFER_SIZE);
    LOG.debug("gRPC read buffer size (count): %d", streamingBufferSize);
    retryOptionsBuilder.setStreamingBufferSize(streamingBufferSize);

    int maxScanTimeoutRetries = configuration.getInt(MAX_SCAN_TIMEOUT_RETRIES,
        RetryOptions.DEFAULT_MAX_SCAN_TIMEOUT_RETRIES);
    LOG.debug("gRPC max scan timeout retries (count): %d", maxScanTimeoutRetries);
    retryOptionsBuilder.setMaxScanTimeoutRetries(maxScanTimeoutRetries);

    return retryOptionsBuilder.build();
  }
}
