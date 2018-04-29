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

import com.google.bigtable.admin.v2.Cluster;
import com.google.bigtable.admin.v2.CreateInstanceRequest;
import com.google.bigtable.admin.v2.Instance;
import com.google.bigtable.admin.v2.Instance.Type;
import com.google.bigtable.admin.v2.ListInstancesRequest;
import com.google.bigtable.admin.v2.ListInstancesResponse;
import com.google.cloud.bigtable.config.BigtableOptions;
import com.google.cloud.bigtable.config.BigtableVersionInfo;
import com.google.cloud.bigtable.config.CredentialOptions;
import com.google.cloud.bigtable.config.Logger;
import com.google.cloud.bigtable.config.RetryOptions;
import com.google.cloud.bigtable.grpc.BigtableInstanceGrpcClient;
import com.google.cloud.bigtable.grpc.io.ChannelPool;
import com.google.cloud.bigtable.grpc.io.CredentialInterceptorCache;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

public class CreateInstanceMain{
  /** Constant <code>LOG.</code> */
  protected static final Logger LOG = new Logger(CreateInstanceMain.class);
  
  private static final int PORT = 443;
  private static final String DEFAULT_HOST = "bigtableadmin.googleapis.com";
  
  
  // 256 MB, server has 256 MB limit.
  private static final int MAX_MESSAGE_SIZE = 1 << 28;
  
  private static BigtableOptions options;
  private static BigtableInstanceGrpcClient instanceClient;
  private static String projectId;
  private static String instanceId;
  private static String location;
  private static String displayName;
  private static String clusterName;
  private static String instanceType;
  private static String LOCATION_FORMAT;
  private static String PARENT_FORMAT;
  
  public static void main(String[] args) throws Exception {
    //Project Id under which instance will be created.
    projectId = args[0];
    LOG.info("projectId:" + projectId);
    
    //Instance id to be created.
    instanceId = args[1];
    LOG.info("instanceId:" + instanceId);
    
    //provide zone. store your data near the services that need it.
    location = args[2];
    
    //display name for an instance.
    displayName = args[3];
    
    //cluster name.
    clusterName = args[4];
    
    //instance type.
    instanceType = args[5];
    
    //zone format API understands. 
    LOCATION_FORMAT = "projects/" + projectId + "/locations/" + location;
    PARENT_FORMAT = "projects/" + projectId;
    
    CreateInstanceMain main = new CreateInstanceMain();
    main.execute();
  }
  
  private void execute() throws IOException {
    init();
    createInstance();
    listInstances();
  }
  
  private void init() throws IOException  {
    options = BigtableOptionsFactoryCreateInstance
        .fromConfiguration(new Configuration());
    
    //Create GRPC client.
    instanceClient = 
        new BigtableInstanceGrpcClient(createChannelPool(DEFAULT_HOST, 1));
  }
  
  private void createInstance() throws IOException {
    CreateInstanceRequest.Builder request = CreateInstanceRequest.newBuilder();
    request.setInstanceId(instanceId);
    request.setParent(PARENT_FORMAT);
    
    Instance.Builder instanceBuilder = Instance.newBuilder();
    if (instanceType.equals("PRODUCTION")) {
      instanceBuilder.setType(Type.PRODUCTION);
    } else {
      instanceBuilder.setType(Type.DEVELOPMENT);
    }
    instanceBuilder.setDisplayName(displayName);
    request.setInstance(instanceBuilder.build());
    
    Cluster.Builder cluster = Cluster.newBuilder();
    cluster.setName(clusterName);
    cluster.setLocation(LOCATION_FORMAT);
    request.putClusters("cluster1", cluster.build());
    
    instanceClient.createInstance(request.build());
  }
  
  private void listInstances() {
    ListInstancesRequest.Builder request = ListInstancesRequest.newBuilder();
    request.setParent(PARENT_FORMAT);
    ListInstancesResponse response = instanceClient.listInstances(request.build());
    List<Instance> instances = response.getInstancesList();
    for (Instance instance : instances) {
      LOG.info("Instance:" + instance.getName());
    }
  }
  
  private ClientInterceptor[] createInterceptors() throws IOException {
    List<ClientInterceptor> clientInterceptorsList = new ArrayList<>();
    // Looking up Credentials takes time. Creating the retry executor and the EventLoopGroup 
    //don't take as long, but still take time. Get the credentials on one thread, and start 
    //up the elg and scheduledRetries thread pools on another thread.
    CredentialInterceptorCache credentialsCache = CredentialInterceptorCache.getInstance();
    RetryOptions retryOptions = options.getRetryOptions();
    CredentialOptions credentialOptions = options.getCredentialOptions();
    try {
      ClientInterceptor credentialsInterceptor =
          credentialsCache.getCredentialsInterceptor(credentialOptions, retryOptions);
      if (credentialsInterceptor != null) {
        clientInterceptorsList.add(credentialsInterceptor);
      }
    } catch (GeneralSecurityException e) {
      throw new IOException("Could not initialize credentials.", e);
    }

    ClientInterceptor[] clientInterceptors =
        clientInterceptorsList.toArray(new ClientInterceptor[clientInterceptorsList.size()]);
    return clientInterceptors;
  }
  
  private ChannelPool createChannelPool(final String hostString, int count) throws IOException {
    ChannelPool.ChannelFactory channelFactory = new ChannelPool.ChannelFactory() {
      @Override
      public ManagedChannel create() throws IOException {
        return createNettyChannel(hostString, createInterceptors());
      }
    };
    return new ChannelPool(channelFactory, count);
  }
  
  public static ManagedChannel createNettyChannel(String host,
      ClientInterceptor ... interceptors) throws SSLException {

    // Ideally, this should be ManagedChannelBuilder.forAddress(...) rather than an explicit
    // call to NettyChannelBuilder.  Unfortunately, that doesn't work for shaded artifacts.
    ManagedChannelBuilder<?> builder = ManagedChannelBuilder
        .forAddress(host, PORT);

    return builder
        .idleTimeout(Long.MAX_VALUE, TimeUnit.SECONDS)
        .maxInboundMessageSize(MAX_MESSAGE_SIZE)
        .userAgent(BigtableVersionInfo.CORE_UESR_AGENT + ",hbase-2.0.0-beta-2")
        .intercept(interceptors)
        .build();
  }
}
