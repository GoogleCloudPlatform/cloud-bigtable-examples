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
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.util.Properties;
import java.util.Map;
import java.util.NavigableMap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.HttpMethodConstraint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name="json", urlPatterns={"/json/*"} ) // asyncSupported=true
@ServletSecurity(
  httpMethodConstraints={
    @HttpMethodConstraint("GET"),
    @HttpMethodConstraint("DELETE"),
    @HttpMethodConstraint("POST"),
    @HttpMethodConstraint("OPTIONS") } )

public class JsonServlet extends HttpServlet {
  private static final TableName TABLE = TableName.valueOf("from-json");
  private static final String cf1 = "cf1";
  private static final byte[] CF1 = Bytes.toBytes(cf1);

// The initial connection to Cloud Bigtable is an expensive operation -- We cache this Connection
// to speed things up.  For this sample, keeping them here is a good idea, for
// your application, you may wish to keep this somewhere else.
  public static Connection connection = null;     // The authenticated connection

  public JsonServlet() {
    super();
  }

/**
 * doGet() - for a given row, put all values into a simple JSON request.
 * (basically a simple map<key,v>)
 *
 * Column Family CF1 is well known, so we don't mention it in the JSON output, any additional
 * families will be encoded as family:col, which is valid JSON, but not valid javascript.
 *
 * This is fairly simple code, so if there is a column with invalid syntax for JSON, there will be
 * an error.
 * Bigtable (and hbase) fields are basically blobs, so I've chosen to recognize a few "datatypes"
 * bool is defined as 1 byte (either 0x00 / 0x01) for (false / true)
 * counter (long) 64 bits 8 bytes and the first 4 bytes are either 0 or -128
 * doubles we will keep as text in Bigtable / hbase
 **/
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
  throws ServletException, IOException {
    String path = req.getPathInfo();
    String key;
    JSONObject json = new JSONObject();
    if (path.length() < 5) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      log("doGet-bad length-"+path.length());
      return;
    }

// IMPORTANT - This try() is a java7 try w/ resources which will close() the table at the end.
    try(Table t = BigtableHelper.getConnection().getTable( TABLE )) {

      Get g = new Get(Bytes.toBytes(path.substring(1)));
      g.setMaxVersions(1);
      Result r = t.get(g);
      log(r.toString());
      NavigableMap<byte[],NavigableMap<byte[],byte[]>> map = r.getNoVersionMap();

      if(map == null) {
        resp.setContentType("application/json");
        resp.getWriter().print("{}");
        return;
      }
      // For Every Family
      for (Map.Entry<byte[], NavigableMap<byte[],byte[]>> family : map.entrySet()) {
        String cFamily = Bytes.toString(family.getKey());
        // Each column
        for(Map.Entry<byte[], byte[]> entry : family.getValue().entrySet()) {
          String col = Bytes.toString(entry.getKey());
          byte[] val = entry.getValue();
          if (cFamily.equals(cf1)) {
            key = col;
          } else {
            key = cFamily + ":" + col;
          }
          // Based on data type, create the json.  8 bytes (leading 0) is an int (counter)
          // 1 byte (0/1) = (false/true) else string
          switch(val.length) {
          case 8:
            if ((val[0] == 0 && val[1] == 0 && val[2] == 0 && val[3] == 0) ||
                (val[0] == -1 && val[1] == -1 && val[2] == -1 && val[3] == -1)) {
              json.put(key, Bytes.toLong(val));
            } else {
              String temp = Bytes.toString(val);
              if (isNumeric(temp)) {
                if(isDigits(temp)) {
                  json.put(key, Long.parseLong(temp));
                } else {
                  json.put(key, Double.parseDouble(temp));
                }
              } else {
                json.put(key, temp);
              }
            }
            break;
          case 1:
            switch (val[0]) {
            case 0:
                json.put(key, false);
              continue;
            case 1:
                json.put(key, true);
              continue;
            default:
            }
          default:
            String temp = Bytes.toString(val);
            if (isNumeric(temp)) {
              if(isDigits(temp)) {
                json.put(key, Long.parseLong(temp));
              } else {
                json.put(key, Double.parseDouble(temp));
              }
            } else {
              json.put(key, temp);
            }
          }
        }
      }
      resp.addHeader("Access-Control-Allow-Origin", "*");
      resp.setContentType("application/json");
      json.write(resp.getWriter());
    } catch (Exception e) {
      log("doGet", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    resp.setStatus(HttpServletResponse.SC_OK);
  }

/**
 * doPost() - for a given row, get all values from a simple JSON request.
 * (basically a simple map<key,v>)
 *
 * Column Family CF1 is well known and will be what we add columns to that don't have a ':' column
 * prefix.  If you specify a column family, it should have been created as we aren't creating them
 * here.
 *
 * Bigtable (and hbase) fields are basically blobs, so I've chosen to recognize a few "datatypes"
 * bool is defined as 1 byte (either 0x00 / 0x01) for (false / true)
 * counter (long) 64 bits 8 bytes and the first 4 bytes are either 0 or -128
 * doubles we will keep as text in Bigtable / hbase
 *
 * Since we are in a managed VM, we could use a bufferedMutator
 **/
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
  throws ServletException, IOException {
    String path = req.getPathInfo();

    if (path.length() < 5) {
      log("doPost-bad length-"+path.length());
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

// IMPORTANT - This try() is a java7 try w/ resources which will close() the table at the end.
    try(Table t = BigtableHelper.getConnection().getTable( TABLE )) {
      JSONObject json = new JSONObject(new JSONTokener(req.getReader()));
      String[] names = JSONObject.getNames(json);

      Put p = new Put(Bytes.toBytes(path.substring(1)));
      for(String key : names) {
        byte[] col = CF1;
        String[] k = key.split(":");    // Some other column family?
        if(k.length > 1) {
          col = Bytes.toBytes(k[0]);
          key = k[1];
        }
        Object o = json.opt(key);
        if (o == null) {
          continue;       // skip null's for Bigtable / hbase
        }
        switch (o.getClass().getSimpleName()) {
        case "Boolean":
         p.addColumn( col, Bytes.toBytes(key), Bytes.toBytes((boolean) o));
          break;

        case "String":
          p.addColumn( col, Bytes.toBytes(key), Bytes.toBytes((String) o));
          break;

        case "Double":  // Store as Strings
          p.addColumn( col, Bytes.toBytes(key), Bytes.toBytes(o.toString()));
          break;

        case "Long":
          p.addColumn( col, Bytes.toBytes(key), Bytes.toBytes((long) o));
          break;
        case "Integer":
          long x = ((Integer) o);
          p.addColumn( col, Bytes.toBytes(key), Bytes.toBytes(x));
          break;
        }
      }
      t.put(p);
    } catch (Exception io) {
      log("Json", io);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    resp.addHeader("Access-Control-Allow-Origin", "*");
    resp.setStatus(HttpServletResponse.SC_OK);
  }

/**
 * doDelete - Delete REST verb -- deletes the entire ROW -- All Versions.
 **/
  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
  throws ServletException, IOException {
    String path = req.getPathInfo();
    log("doDelete-"+path);
    if (path.length() < 5) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      log("doDelete-bad length-"+path+"("+path.length()+")");
      return;
    }

// IMPORTANT - This try() is a java7 try w/ resources which will close() the table at the end.
    try(Table t = BigtableHelper.getConnection().getTable( TABLE )) {
      resp.setContentType("application/json");
      JSONObject json = new JSONObject();
      Delete d = new Delete( Bytes.toBytes(path.substring(1)) );  // Delete the ROW
      t.delete(d);
    } catch (Exception e) {
      log("doDelete", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    resp.addHeader("Access-Control-Allow-Origin", "*");
    resp.setStatus(HttpServletResponse.SC_OK);
  }

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
      log("doOptions");
      // pre-flight request processing
      resp.setHeader("Access-Control-Allow-Origin", "*");
      resp.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
  }

  public boolean isNumeric(String s) {
      return s.matches("[-+]?\\d*\\.?\\d+");
  }

  public boolean isDigits(String s) {
      return s.matches("\\d+");
  }

}
