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

package com.example.cloud.bigtable.helloworld;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.lang.String;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {
  private static final TableName TABLE = TableName.valueOf("gae-hello");

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

// IMPORTANT - this try() is a java7 try w/ resources, which will call close() when done.
    try (Table t = BigtableHelper.getConnection().getTable( TABLE )) {

      // incrementColumnValue(row, family, column qualifier, amount)
      result = t.incrementColumnValue(Bytes.toBytes(id), Bytes.toBytes("visits"),
              Bytes.toBytes("visits"), 1);
    } catch (IOException e) {
      log("getAndUpdateVisit", e);
      return "0 error "+e.toString();
    }
    return String.valueOf(result);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String userID = "00001";
    String tok = req.getParameter("idtoken");

    if (tok != null) {
      userID = tok;
    } else {
      log("Invalid ID token.");
    }

    resp.getWriter().print(getAndUpdateVisit(userID));
  }
}
