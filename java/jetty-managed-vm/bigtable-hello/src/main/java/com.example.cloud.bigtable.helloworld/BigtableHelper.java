/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.cloud.bigtable.helloworld;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * BigtableHelper, a ServletContextListener, is setup in web.xml to run before a JSP is run.
 *
 **/
public class BigtableHelper implements ServletContextListener {
/**
 * You need to set your PROJECT_ID, CLUSTER_UNIQUE_ID (typically 'cluster') here, and if different,
 * your Zone.
 **/
  private static final String PROJECT_ID = "PROJECT_ID_HERE";
  private static final String CLUSTER_ID = "CLUSTER_UNIQUE_ID";

  private static final String ZONE = "us-central1-b";

// The initial connection to Cloud Bigtable is an expensive operation -- We cache this Connection
// to speed things up.  For this sample, keeping them here is a good idea, for
// your application, you may wish to keep this somewhere else.
  private static Connection connection = null;     // The authenticated connection

  private static ServletContext sc;

/**
 * Connect will establish the connection to Cloud Bigtable.
 **/
  public static void connect() throws IOException {
    Configuration c = HBaseConfiguration.create();

    c.setClass("hbase.client.connection.impl",
        com.google.cloud.bigtable.hbase1_1.BigtableConnection.class,
        org.apache.hadoop.hbase.client.Connection.class);   // Required for Cloud Bigtable
    c.set("google.bigtable.endpoint.host", "bigtable.googleapis.com");
    c.set("google.bigtable.admin.endpoint.host", "bigtabletableadmin.googleapis.com");

    c.set("google.bigtable.project.id", PROJECT_ID);
    c.set("google.bigtable.cluster.name", CLUSTER_ID);
    c.set("google.bigtable.zone.name", ZONE);

    connection = ConnectionFactory.createConnection(c);
  }

  public static Connection getConnection() {
    if(connection == null) {
      try {
        connect();
      } catch (IOException e) {
        sc.log("connect ", e);
      }
    }
    if(connection == null) sc.log("BigtableHelper-No Connection");
    return connection;
  }

  public void contextInitialized(ServletContextEvent event) {
    // This will be invoked as part of a warmup request, or the first user
    // request if no warmup request was invoked.
    sc = event.getServletContext();
    try {
      connect();
    } catch (IOException e) {
        sc.log("BigtableHelper - connect ", e);
    }
     if(connection == null) sc.log("BigtableHelper-No Connection");
 }

  public void contextDestroyed(ServletContextEvent event) {
    // App Engine does not currently invoke this method.
    try {
      connection.close();
    } catch(IOException io) {
      sc.log("contextDestroyed ", io);
    }
    connection = null;
  }
}
