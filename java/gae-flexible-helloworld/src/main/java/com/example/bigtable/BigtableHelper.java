/**
 * Copyright 2015 Google Inc.
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
package com.example.bigtable;

import com.google.cloud.bigtable.hbase.BigtableConfiguration;
import org.apache.hadoop.hbase.client.Connection;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * BigtableHelper, a ServletContextListener, is setup in web.xml to run before a JSP is run.
 * Project / Instance settings can be passed as an Environment Variable, a System Property, or set
 * in web.xml from a context-param
 *
 **/
@WebListener
public class BigtableHelper implements ServletContextListener {

  private static String PROJECT_ID = System.getenv("BIGTABLE_PROJECT");
  private static String INSTANCE_ID = System.getenv("BIGTABLE_INSTANCE");

// The initial connection to Cloud Bigtable is an expensive operation -- We cache this Connection
// to speed things up.  For this sample, keeping them here is a good idea, for
// your application, you may wish to keep this somewhere else.
  private static Connection connection = null;     // The authenticated connection

  private static ServletContext sc;

/**
 * Connect will establish the connection to Cloud Bigtable.
 **/
  public static void connect() throws IOException {

    if (PROJECT_ID == null || INSTANCE_ID == null ) {
      sc.log("environment variables BIGTABLE_PROJECT, and BIGTABLE_INSTANCE need to be defined.");
      return;
    }

    connection = BigtableConfiguration.connect(PROJECT_ID, INSTANCE_ID);
  }

  public static Connection getConnection() {
    if (connection == null) {
      try {
        connect();
      } catch (IOException e) {
        sc.log("connect ", e);
      }
    }
    if (connection == null) {
      sc.log("BigtableHelper-No Connection");
    }
    return connection;
  }

  public void contextInitialized(ServletContextEvent event) {
    // This will be invoked as part of a warmup request, or the first user
    // request if no warmup request was invoked.

    if (PROJECT_ID != null && PROJECT_ID.startsWith("$")) {
      PROJECT_ID = null;
    }
    if (INSTANCE_ID != null && INSTANCE_ID.startsWith("$")) {
      INSTANCE_ID = null;
    }

    if (event != null) {
      sc = event.getServletContext();
      if (PROJECT_ID == null) {
        PROJECT_ID = sc.getInitParameter("BIGTABLE_PROJECT");
      }
      if (INSTANCE_ID == null) {
        INSTANCE_ID = sc.getInitParameter("BIGTABLE_INSTANCE");
      }
    }

    if (PROJECT_ID == null) {
      PROJECT_ID = System.getProperty("BIGTABLE_PROJECT");
    }
    if (INSTANCE_ID == null) {
      INSTANCE_ID = System.getProperty("BIGTABLE_INSTANCE");
    }

    try {
      connect();
    } catch (IOException e) {
      sc.log("BigtableHelper - connect ", e);
    }
    if (connection == null) {
      sc.log("BigtableHelper-No Connection");
    }
  }

  public void contextDestroyed(ServletContextEvent event) {
    // App Engine does not currently invoke this method.
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (IOException io) {
      sc.log("contextDestroyed ", io);
    }
    connection = null;
  }
}
