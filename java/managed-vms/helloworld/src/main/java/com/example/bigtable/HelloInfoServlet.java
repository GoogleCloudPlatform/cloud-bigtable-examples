/*
 * Copyright (c) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.example.bigtable;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloInfoServlet extends HttpServlet {
  Logger logger = LoggerFactory.getLogger("com.example.bigtable.HelloInfoServlet");

// The initial connection to Cloud Bigtable is an expensive operation -- We cache this Connection
// to speed things up.  For this sample, keeping them here is a good idea, for
// your application, you may wish to keep this somewhere else.
  public static Connection connection = null;     // The authenticated connection

/**
 * init() in this case is called at Deploy time as this servlet is marked to load-on-start.  The
 * only thing it does is setup the connection to the Cloud Bigtable server.
 **/
  @Override
  public void init() {
    try {
      connect();
    } catch (IOException io) {
      logger.error("init ***"+io.toString());
      io.printStackTrace();
    }
  }

/**
 * destroy() is called when the Servlet container (jetty) wants to get rid if this, typically when
 * shutting down.  We'll get rid of our connection here.
 **/
  @Override
  public void destroy() {
    try {
      if(connection != null) connection.close();
    } catch (IOException io) {
      logger.error("destroy ***"+io.toString());
      io.printStackTrace();
    }
    connection = null;
  }

/**
 * Connect will establish the connection to Cloud Bigtable.  You need to set your PROJECT_ID,
 * CLUSTER_UNIQUE_ID (typically 'cluster') here, and if different, your Zone.
 **/
  public void connect() throws IOException {
    Configuration c = HBaseConfiguration.create();

    c.setClass("hbase.client.connection.impl",
        org.apache.hadoop.hbase.client.BigtableConnection.class,
        org.apache.hadoop.hbase.client.Connection.class);   // Required for Cloud Bigtable
    c.set("google.bigtable.endpoint.host", "bigtable.googleapis.com");

    c.set("google.bigtable.project.id","PROJECT_ID_HERE");
    c.set("google.bigtable.cluster.name","CLUSTER_UNIQUE_ID");
    c.set("google.bigtable.zone.name", "us-central1-b");  // ZONE NAME IF DIFFERENT

    connection = ConnectionFactory.createConnection(c);
  }

/**
 * getAndUpdateVisit will just increment and get the visits:visits column, using
 * incrementColumnValue does the equivalent of locking the row, getting the value, incrementing
 * the value, and writing it back.  Also, unlike most other APIs, the column is assumed to have
 * a Counter data type (actually very long as it's 8 bytes)
 *
 * You will note that we get a new table with each request, this is a lightweight operation and it
 * is preferred to caching.
 **/
  public String getAndUpdateVisit(String id) throws IOException {
    long result;

    if( connection == null ) connect();

    try {
      Table t = connection.getTable(TableName.valueOf("gae-hello"));
      // incrementColumnValue(row, family, column qualifier, amount)
      result = t.incrementColumnValue(Bytes.toBytes(id), Bytes.toBytes("visits"),
                                              Bytes.toBytes("visits"), 1);
      t.close(); // We are done with our table.
    } catch (IOException e) {
      e.printStackTrace();
      return "0 error "+e.toString();
    }
    return String.valueOf(result);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    User currentUser = userService.getCurrentUser();

    if (currentUser != null) {
      resp.setContentType("text/plain");
      resp.getWriter().println("Hello, " + currentUser.getNickname());
      resp.getWriter().println("You have visited " + getAndUpdateVisit(currentUser.getUserId()));
    } else {
      resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
    }
  }
}
