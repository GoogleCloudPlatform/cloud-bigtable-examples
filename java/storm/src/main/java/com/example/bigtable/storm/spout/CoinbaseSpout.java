/**
 * Copyright 2015 Google Inc. All Rights Reserved.
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

package com.example.bigtable.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Time;
import com.example.bigtable.storm.data.CoinbaseData;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a spout that reads from the Coinbase websocket feed, and emits
 * it as a Storm spout to the Storm stream.
 */
public class CoinbaseSpout extends BaseRichSpout {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSpout
            .class);


    /**
     * Standard Storm collector used to emit our Coinbase data to a stream.
     */
    private SpoutOutputCollector mCollector;

    /**
     * We push the incoming market data onto this queue as it comes in from
     * the coinbase API, and serve it up to the Storm topology when
     * nextTuple() is called.
     */
    private LinkedBlockingQueue<CoinbaseData> mQueue = null;

    /**
     * Capacity of our in memory queue.
     */
    private static final int QUEUE_CAPACITY = 1000;


    /**
     * Number of milliseconds to sleep if we poll an empty queue,
     * this way we avoid excessive spinning if there's no incoming data.
     */
    private static final int QUEUE_BACKOFF_MS = 50;

    /**
     * Here we declare that we emit the entire CoinbaseData POJO to the stream.
     */
    @Override
    public void declareOutputFields(final OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("coin"));
    }

    /**
     * Establish the connection to the Coinbase Websocket feed.
     * @param map
     * @param topologyContext
     * @param collector
     */
    @Override
    public void open(Map map, TopologyContext topologyContext,
                     SpoutOutputCollector collector) {
        mQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        mCollector = collector;

        String destUri = "wss://ws-feed.exchange.coinbase.com";
        WebSocketClient client = new WebSocketClient(new SslContextFactory());
        CoinbaseSocket socket = new CoinbaseSocket(mQueue);
        try {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Get data from our Coinbase market queue and emit it to the Storm stream.
     */
    @Override
    public void nextTuple() {
        CoinbaseData data = mQueue.poll();
        if (data == null) {
            try {
                Time.sleep(QUEUE_BACKOFF_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            mCollector.emit(new Values(data));
        }
    }
}
