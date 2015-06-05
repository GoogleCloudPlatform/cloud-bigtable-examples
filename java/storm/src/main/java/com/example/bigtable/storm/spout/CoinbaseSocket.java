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


/*@WebSocket(maxTextMessageSize = 64 * 1024)*/
/**
 * This class provides a Websocket that connections to the Coinbase
 * WebSocket API and pushes received messages onto a mQueue passed into its
 * constructor.
 */
@WebSocket
public class CoinbaseSocket {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSocket
            .class);

    private final CountDownLatch mCloseLatch;

    @SuppressWarnings("unused")
    private Session mSession;

    /**
     * Send this object as soon as we connect to the Coinbase Market API
     * to signify we want to subscribe to the BTC-USD feed.
     */
    private static final String COINBASE_SUBSCRIBE_MESSAGE = "{\n"
            + "    \"type\": \"subscribe\",\n"
            + "    \"product_id\": \"BTC-USD\"\n"
            + "}";

    /**
     * This mQueue is provided to us by the Storm topology to push data as we
     * receive it from Coinbase so that it can be polled as nextTuple() is
     * called in a Storm Spout.
     */
    private Queue mQueue;

    // Used to serialize Coinbasedata from JSON into our CoinbaesData POJO.
    private ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Creates teh socket with the queue to add to that the spout will read
     * from.
     * @param queue This is the queue that incoming data gets added to
     */
    public CoinbaseSocket(Queue<CoinbaseData> queue) {
        mCloseLatch = new CountDownLatch(1);
        mQueue = queue;
    }

    /**
     * This method can be used by other functions if they want to await for
     * a proper shutdown of the web socket.
     * @param duration How long to wait until shutdown.
     * @param unit Unit of duration
     * @throws InterruptedException InterruptedException
     * @return True if the socket is closed before the timeout
     */
    public boolean awaitClose(int duration, TimeUnit unit)
            throws InterruptedException {
        return mCloseLatch.await(duration, unit);
    }


    /**
     * Handler to cleanup on close.
     * @param statusCode Status code of closing
     * @param reason Reason the socket is being closed
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        mSession = null;
        mCloseLatch.countDown();
    }

    /**
     * Upon connection to the Coinbase Websocket market feed, we send our
     * subscription message to indicate the product we want to subscribe to.
     * @param session Websocket session
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        System.out.printf("Got connect: %s%n", session);
        mSession = session;
        try {
            Future<Void> fut;
            fut = session.getRemote()
                    .sendStringByFuture(COINBASE_SUBSCRIBE_MESSAGE);
            fut.get(2, TimeUnit.SECONDS);
            } catch (Throwable t) {
              t.printStackTrace();
            }
    }

    /**
     * Here we recive a Coinbase message and push it onto our mQueue.
     * @param msg The coinbase market data feed
     * https://docs.exchange.coinbase.com/#websocket-feed
     */
    @OnWebSocketMessage
    public void onMessage(String msg) {
        try {
            CoinbaseData data = objectMapper.readValue(msg, CoinbaseData
                    .class);
            mQueue.offer(data);
        } catch (IOException e) {
            e.printStackTrace();
            LOG.info("bad data was: " + msg);
        }
    }
}
