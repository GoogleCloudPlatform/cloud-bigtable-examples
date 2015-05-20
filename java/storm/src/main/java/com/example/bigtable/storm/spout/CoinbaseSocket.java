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

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.example.bigtable.storm.data.CoinbaseData;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a Websocket that connections to the Coinbase
 * WebSocket API and pushes received messages onto a queue passed into its
 * constructor.
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class CoinbaseSocket {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSocket
            .class);

    private final CountDownLatch closeLatch;

    @SuppressWarnings("unused")
    private Session session;

    /**
     * We send this object as soon as we connect to the Coinbase Market API
     * to signify we want to subscribe to the BTC-USD feed.
     */
    String send = "{\n" +
            "    \"type\": \"subscribe\",\n" +
            "    \"product_id\": \"BTC-USD\"\n" +
            "}";

    /**
     * This queue is provided to us by the Storm topology to push data as we
     * receive it from Coinbase so that it can be polled as nextTuple() is
     * called in a Storm Spout.
     */
    private Queue queue;

    // Used to serialize Coinbasedata from JSON into our CoinbaesData POJO.
    private ObjectMapper objectMapper = new ObjectMapper();

    public CoinbaseSocket(Queue<CoinbaseData> queue) {
        this.closeLatch = new CountDownLatch(1);
        this.queue = queue;
    }

    /**
     * This method can be used by other functions if they want to await for
     * a proper shutdown of the web socket.
     */
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
        }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

    /**
     * Upon connection to the Coinbase Websocket market feed, we send our
     * subscription message to indicate the product we want to subscribe to.
     * @param session
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        this.session = session;
        try {
            Future<Void> fut;
            fut = session.getRemote().sendStringByFuture(send);
            fut.get(2, TimeUnit.SECONDS);
            } catch (Throwable t) {
              t.printStackTrace();
            }
    }

    /**
     * Here we recive a Coinbase message and push it onto our queue.
     * @param msg The coinbase market data feed
     * https://docs.exchange.coinbase.com/#websocket-feed
     */
    @OnWebSocketMessage
    public void onMessage(String msg) {
        try {
            CoinbaseData data = objectMapper.readValue(msg, CoinbaseData
                    .class);
            queue.offer(data);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.info("bad data was: " + msg);
        }
    }
}
