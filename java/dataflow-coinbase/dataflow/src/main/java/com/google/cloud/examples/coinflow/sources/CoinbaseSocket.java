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

package com.google.cloud.examples.coinflow.sources;

import java.io.IOException;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.*;

import com.google.cloud.dataflow.sdk.io.UnboundedSource;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class provides a Websocket that connections to the Coinbase
 * WebSocket API and pushes received messages onto a queue, which then
 * serves as a source for Cloud Dataflow
 */
@WebSocket
public class CoinbaseSocket extends UnboundedSource.UnboundedReader<String> {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSocket
            .class);

    private CoinbaseSource mCoinbaseSource;
    private String mCurrent;
    private Instant mCurrentTimestamp;


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
    private Queue<String> mQueue;


    /**
     * Create the socket
     * @param coinbaseSource This is the Dataflow source for the getCurrentSource call
     */
    public CoinbaseSocket(CoinbaseSource coinbaseSource) {
        LOG.info("socket created");
        mQueue = new LinkedBlockingQueue<>();
        mCoinbaseSource = coinbaseSource;
    }

    /**
     * Handler to cleanup on close.
     * @param statusCode Status code of closing
     * @param reason Reason the socket is being closed
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOG.info("Connection closed: %d - %s%n", statusCode, reason);
    }

    /**
     * Upon connection to the Coinbase Websocket market feed, we send our
     * subscription message to indicate the product we want to subscribe to.
     * @param session Websocket session
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        LOG.info("Got connect: %s%n", session);
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
        LOG.debug("got coinbase msg ", msg);
        mQueue.offer(msg);
    }

    @Override
    public boolean start() throws IOException {
        String destUri = "wss://ws-feed.exchange.coinbase.com";
        WebSocketClient client = new WebSocketClient(new SslContextFactory());
        try {
            LOG.info("connecting to coinbsae feed");
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(this, echoUri, request);
            LOG.info("done connecting");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return advance();

    }

    @Override
    public boolean advance() throws IOException {
        mCurrent = mQueue.poll();
        mCurrentTimestamp = Instant.now();
        return (mCurrent != null);
    }

    @Override
    public String getCurrent() throws NoSuchElementException {
        if (mCurrent == null) {
            throw new NoSuchElementException();
        }
        return mCurrent;
    }

    @Override
    public Instant getCurrentTimestamp() throws NoSuchElementException {
        if (mCurrent == null) {
            throw new NoSuchElementException();
        }
        return mCurrentTimestamp;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public byte[] getCurrentRecordId() throws NoSuchElementException {
        return new byte[0];
    }

    @Override
    public Instant getWatermark() {
        return mCurrentTimestamp.minus(new Duration(1));
    }

    @Override
    public UnboundedSource.CheckpointMark getCheckpointMark() {
        return new UnboundedSource.CheckpointMark() {
            @Override
            public void finalizeCheckpoint() throws IOException {

            }
        };
    }

    @Override
    public CoinbaseSource getCurrentSource() {
        return mCoinbaseSource;
    }

}
