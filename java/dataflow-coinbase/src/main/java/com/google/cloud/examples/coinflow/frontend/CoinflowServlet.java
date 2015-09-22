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

package com.google.cloud.examples.coinflow.frontend;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.hbase.util.Bytes;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


public class CoinflowServlet extends HttpServlet {
    private static final TableName TABLE = TableName.valueOf("Dataflow_test10");

    private static final Logger LOG = LoggerFactory.getLogger(CoinflowServlet
            .class);

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

    public List<String> getRows() throws IOException {
        try (Table t = BigtableHelper.getConnection().getTable( TABLE )) {


        } catch (IOException e) {
            log("getRows", e);
            return null;
        }
        return null;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.info("In CoinflowServlet doGet");
        UserService userService = UserServiceFactory.getUserService();
        User currentUser = userService.getCurrentUser();

        if(req.getRequestURI().equals("/favicon.ico")) return;

        if (currentUser != null) {
            resp.setContentType("text/plain");
            resp.getWriter().println("Hello now3, " + currentUser.getNickname());
            /*try(Table t = BigtableHelper.getConnection().getTable( TABLE )) {

                DateTime dateTime = new DateTime().minusHours(4);
                long before_ts = dateTime.getMillis();
                long now_ts = new DateTime().getMillis();
                String beforeRowKey = "match";
                String afterRowKey = "matci";
                Scan scan = new Scan(beforeRowKey.getBytes(), afterRowKey.getBytes());
                ResultScanner rs = t.getScanner(scan);
                resp.addHeader("Access-Control-Allow-Origin", "*");
                resp.setContentType("text/plain");
                resp.getWriter().println("Got here at least with new keys");
                //json.write(resp.getWriter());
                for (Result r : rs) {
                    resp.getWriter().println("Result is " + r);
                }
            }*/
 resp.getWriter().println("You have visited " + getAndUpdateVisit
              (currentUser.getUserId()));

        } else {
            resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
        }
    }
}
