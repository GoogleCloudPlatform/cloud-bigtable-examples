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
import com.google.bigtable.admin.v2.CreateClusterRequest;
import com.google.bigtable.admin.v2.CreateInstanceRequest;
import com.google.bigtable.admin.v2.DeleteInstanceRequest;
import com.google.bigtable.admin.v2.Instance;
import com.google.bigtable.admin.v2.Instance.Type;
import com.google.bigtable.admin.v2.ListInstancesRequest;
import com.google.bigtable.admin.v2.ListInstancesResponse;

import com.google.cloud.bigtable.config.Logger;
import com.google.cloud.bigtable.grpc.BigtableInstanceClient;
import com.google.cloud.bigtable.grpc.BigtableSession;

import java.io.IOException;

/*
 * This sample code illustrates how to create Bigtable instance
 * using Bigtable APIs.
 */
public class CreateInstanceMain{
  /** Constant <code>LOG.</code> */
  protected static final Logger LOG = new Logger(CreateInstanceMain.class);
  
  private static BigtableInstanceClient instanceClient;
  private static String projectId;
  private static String instanceId;
  private static String location;
  private static String displayName;
  private static String clusterName;
  private static String instanceType;
  private static String locationFormat;
  private static String parentFormat;
  private static Boolean isCreateNewCluster = false;
  private static String newClusterLocation;
  private static String newClusterName;
  private static Integer clusterNodes;
  
  public static void main(String[] args) throws Exception {
    //Project Id under which instance will be created.
    projectId = requiredProperty("bigtable.projectID");
    instanceId = requiredProperty("bigtable.instanceID");
    //Refer below link for location 
    //  https://cloud.google.com/bigtable/docs/locations
    location = requiredProperty("bigtable.location");
    displayName = requiredProperty("bigtable.displayName");
    clusterName = requiredProperty("bigtable.clusterName");
    //provide instance type DEVELOPMENT or PRODUCTION.
    instanceType = requiredProperty("bigtable.instance.type");
    //zone format API understands. 
    locationFormat = "projects/" + projectId + "/locations/" + location;
    parentFormat = "projects/" + projectId;
    
    if (instanceType.equals("PRODUCTION")) {
      clusterNodes = Integer.valueOf(requiredProperty("bigtable.clusterNodes"));
    }
    
    if (requiredProperty("bigtable.createNewCluster").equalsIgnoreCase("true")) {
      setNewClusterProperties();
      LOG.info("cluster " + newClusterName 
          + " will be created under projectId:" + projectId
          + " and zone " + newClusterLocation);
    }
    
    CreateInstanceMain main = new CreateInstanceMain();
    main.execute();
  }
  
  private void execute() {
    try {
      init();
      createInstance();
      listInstances();
      if (isCreateNewCluster) {
        createCluster();
      }
    } catch (Exception e) {
      LOG.error("Exception in create instance.", e);
    } finally {
      deleteInstance();
    }
  }
  
  private void init() throws Exception  {
    instanceClient = BigtableSession.createInstanceClient();;
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
    
    Cluster cluster = null;
    if (instanceType.equals("PRODUCTION")) {
      cluster = setClusterForProduction();
    } else {
      cluster = setClusterForDevelopment();
    }
    
    CreateInstanceRequest request = CreateInstanceRequest.newBuilder()
        .setInstanceId(instanceId)
        .setParent(parentFormat)
        .setInstance(instance)
        .putClusters("cluster1", cluster)
        .build();
    LOG.info("Creating instance " + instanceId);
    instanceClient.createInstance(request);
  }
  
  private Cluster setClusterForProduction() {
    return Cluster.newBuilder()
      .setName(clusterName)
      .setLocation(locationFormat)
      .setServeNodes(clusterNodes)
      .build();
  }
  
  private Cluster setClusterForDevelopment() {
    return Cluster.newBuilder()
      .setName(clusterName)
      .setLocation(locationFormat)
      .build();
  }
  
  private void createCluster() {
    Cluster cluster = Cluster.newBuilder()
        .setServeNodes(clusterNodes)
        .setLocation(newClusterLocation).build();
    
    CreateClusterRequest request = CreateClusterRequest.newBuilder()
        .setCluster(cluster)
        .setClusterId(newClusterName)
        .setParent("projects/" + projectId + "/instances/" + instanceId)
        .build();
    LOG.info("Creating cluster " + newClusterName);
    instanceClient.createCluster(request);
  }
  
  private void listInstances() {
    ListInstancesRequest request = ListInstancesRequest
        .newBuilder()
        .setParent(parentFormat)
        .build();
    ListInstancesResponse response = instanceClient.listInstances(request);
    for (Instance instance : response.getInstancesList()) {
      LOG.info("Instance:" + instance.getName());
    }
  }
  
  private void deleteInstance() {
    LOG.info("Finally deleting instance " + instanceId);
    DeleteInstanceRequest request = DeleteInstanceRequest.newBuilder()
        .setName("projects/" + projectId + "/instances/" + instanceId)
        .build();
    instanceClient.deleteInstance(request);
    LOG.debug("Instance " + instanceId + " deleted.");
  }
  
  private static void setNewClusterProperties() {
    isCreateNewCluster = true;
    newClusterLocation = requiredProperty("bigtable.newclusterlocation");
    newClusterLocation = "projects/" + projectId + "/locations/" + newClusterLocation;
    newClusterName = requiredProperty("bigtable.newclusterName");
  }
  
  private static String requiredProperty(String prop) {
    String value = System.getProperty(prop);
    if (value == null) {
      throw new IllegalArgumentException("Missing required system property: " + prop);
    }
    return value;
  }
}
