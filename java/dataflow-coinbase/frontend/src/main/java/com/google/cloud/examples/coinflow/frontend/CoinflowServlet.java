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

import com.google.cloud.examples.coinflow.data.CoinbaseData;
import com.google.cloud.examples.coinflow.data.Schema;
import com.google.cloud.examples.coinflow.utils.DateHelpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CoinflowServlet extends HttpServlet {
  private static final TableName TABLE = TableName.valueOf("coinbase");

  private static final Logger LOG = LoggerFactory.getLogger(CoinflowServlet.class);

  private static class PriceTimestamp {

    public PriceTimestamp(float price, long timestamp) {
      this.price = price;
      this.timestamp = timestamp;
    }

    public final float price;
    public final long timestamp;
  }

  private ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    LOG.info("In CoinflowServlet doGet");

    if (req.getRequestURI().equals("/favicon.ico")) {
      return;
    }

    try (Table t = BigtableHelper.getConnection().getTable(TABLE)) {

      DateTime dateTime = new DateTime().minusHours(4);
      long beforeMillis = dateTime.getMillis();
      long nowMillis = new DateTime().getMillis();

      String beforeRowKey = "match_" + beforeMillis;
      String afterRowKey = "match_" + nowMillis;

      Scan scan = new Scan(beforeRowKey.getBytes(), afterRowKey.getBytes());
      ResultScanner rs = t.getScanner(scan);
      resp.addHeader("Access-Control-Allow-Origin", "*");
      resp.setContentType("text/plain");

      List<PriceTimestamp> prices = new ArrayList<>();
      for (Result r : rs) {
        String data = new String(r.getValue(Schema.CF.getBytes(), Schema.QUALIFIER.getBytes()));
        CoinbaseData coinData = objectMapper.readValue(data, CoinbaseData.class);
        PriceTimestamp priceTimestamp =
            new PriceTimestamp(
                Float.parseFloat(coinData.getPrice()),
                DateHelpers.convertDateToTime(coinData.getTime()));
        prices.add(priceTimestamp);
      }
      String pricesStr = objectMapper.writeValueAsString(prices);
      resp.getWriter().println(pricesStr);
    }
  }
}
