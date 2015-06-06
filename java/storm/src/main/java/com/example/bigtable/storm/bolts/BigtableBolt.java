/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bigtable.storm.bolts;


import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import com.example.bigtable.storm.data.CoinbaseData;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * This is an example of a Storm Bolt that receives a Coinbase market feed
 * object as a tuple input and insert its into Google Cloud Bigtable.
 *
 * Currently, it uses the order type followed by the timestamp. Timestamps
 * are a natural key to use for time-series data, but since they arrive
 * sequentially there is no ability to shard the computation, so it's better
 * to prefix the timestamp by some other fields in the row key. You can
 * read more about key design here:
 *
 * https://cloud.google.com/bigtable/docs/schema-design
 */
public class BigtableBolt extends BaseRichBolt {

    private static final Logger LOG = LoggerFactory.getLogger(BigtableBolt
            .class);

    // Standard Storm Output collector
    private OutputCollector mCollector;

    /**
     * This is the mConnection to Cloud Bigtable.
     */
    private Connection mConnection;

    /**
     * This is our Google Cloud Bigtable table name, which must be specified
     * in the constructor.
     */
    private String mTableName;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private BufferedMutator mBufferedMutator;

    /**
     * Create a Bigtable bolt that will feed incoming tuples into Cloud
     * Bigtable.
     * @param tableName The name of the Bigtable table. Must have a column
     *                  family named 'bc'
     */
    public BigtableBolt(String tableName) {
        this.mTableName = tableName;
    }

    /**
     * Helper function that converts Coinbase timestamps to milliseconds
     * since epoch.
     * @param date in the Coinbase format to convert to milliseconds
     * @return The time in milliseconds since the epoch specified by the date
     */
    private long convertDateToTime(String date) {
        // chop off Z at end
        date = date.substring(0, date.length() - 1);

        DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        Date result;
        try {
            result = df1.parse(date);
        } catch (ParseException e) {
            LOG.error("erorr trying to parse date: " + date);
            LOG.error(e.getMessage());
            return -1;
        }
        return result.getTime();
    }

    /**
     * Sets up our mConnection to Google Cloud Bigtable.
     */
    @Override
    public void prepare(Map conf, TopologyContext context,
                        OutputCollector collector) {
        mCollector = collector;
        try {
            synchronized (this) {
                mConnection = ConnectionFactory.createConnection();
                mBufferedMutator = mConnection.getBufferedMutator(TableName
                        .valueOf(mTableName));
            }
        } catch (IOException e) {
            LOG.error("Error creating Bigtable exception: ", e);
        }
    }

    /**
     * In this method we take a Coinbase data object from our tuple and
     * insert a row into Bigtable. We construct a row key based on the type
     * of the Coinbase data (such as open, or matched), preceded by an
     * underscore and then a timestamp.
     *
     * See here for more info about the Coinbase market feed:
     * https://docs.exchange.coinbase.com/#websocket-feed
     * @param tuple
     */
    @Override
    public void execute(Tuple tuple) {
        try {
            CoinbaseData data = (CoinbaseData) tuple.getValue(0);
            if (data == null) {
                return;
            }

            String ts = Long.toString(convertDateToTime(data.getTime()));
            String rowKey = data.getType() + "_" + ts;
            String columnFamily = "bc";
            String column = "data";

            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column),
                    Bytes.toBytes(objectMapper.writeValueAsString(data)));
            mBufferedMutator.mutate(put);
            mCollector.ack(tuple);
        } catch (IOException e) {
            LOG.error("Got exception executing Bigtable PUT ", e.getMessage());
        }
    }

    /**
     * This bolt only inserts into Cloud Bigtable, it doesn't emit any streams.
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }

    @Override
    public void cleanup() {
        try {
            mBufferedMutator.close();
            mConnection.close();
        } catch (IOException e) {
            LOG.error("Got exception closing Bigtable mConnection",
                    e.getMessage());
        }
    }
}
