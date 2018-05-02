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
import com.google.cloud.bigtable.config.Logger;
import com.google.cloud.bigtable.grpc.BigtableInstanceGrpcClient;
import com.google.cloud.bigtable.grpc.BigtableSession;
import com.google.cloud.bigtable.grpc.io.ChannelPool;

import java.io.IOException;
import java.util.List;

/*
 * This sample code illustrates how to create Bigtable instance
 * using Bigtable APIs.
 */
public class CreateInstanceMain{
  /** Constant <code>LOG.</code> */
  protected static final Logger LOG = new Logger(CreateInstanceMain.class);
  
  private static final String DEFAULT_HOST = "bigtableadmin.googleapis.com";
  
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
    instanceId = args[1];
    location = args[2];
    displayName = args[3];
    clusterName = args[4];
    instanceType = args[5];
    //zone format API understands. 
    LOCATION_FORMAT = "projects/" + projectId + "/locations/" + location;
    PARENT_FORMAT = "projects/" + projectId;
    
    LOG.info("instanceId " + instanceId + " will be created under projectId:" + projectId);
    
    CreateInstanceMain main = new CreateInstanceMain();
    main.execute();
  }
  
  private void execute() throws Exception {
    init();
    createInstance();
    listInstances();
  }
  
  private void init() throws Exception  {
    BigtableOptions options = new BigtableOptions.Builder().build();
    ChannelPool channelPool = BigtableSession.createChannelPool(DEFAULT_HOST, options);
    instanceClient = 
        new BigtableInstanceGrpcClient(channelPool);
  }
  
  private void createInstance() throws IOException {
    Type type = Type.DEVELOPMENT;
    if (instanceType.equals("PRODUCTION")) {
      type = Type.PRODUCTION;
    }
    
    Instance instance = Instance.newBuilder()
        .setDisplayName(displayName)
        .setType(type)
        .build();
    
    Cluster cluster = Cluster.newBuilder()
        .setName(clusterName)
        .setLocation(LOCATION_FORMAT)
        .build();
    
    CreateInstanceRequest request = CreateInstanceRequest.newBuilder()
        .setInstanceId(instanceId)
        .setParent(PARENT_FORMAT)
        .setInstance(instance)
        .putClusters("cluster1", cluster)
        .build();
    
    instanceClient.createInstance(request);
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
}
