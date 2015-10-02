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

package com.google.cloud.examples.coinflow.data;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

// CHECKSTYLE:OFF


/**
 * This is a POJO object that represents the JSON objects we get from the
 * Coinbase WebSocket streaming API. We use Jackson to deserialize the JSON
 * strings into this object. Some of these fields are not present in every
 * instance of incoming data and so will remain as null.
 *
 * See here for more info about the Coinbase market feed:
 * https://docs.exchange.coinbase.com/#websocket-feed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinbaseData implements Serializable {

    private String type;
    private String side;
    private String order_id;
    private String remaining_size;
    private String product_id;
    private String time;
    private String sequence;
    private String price;
    private String reason;
    private String size;
    private String client_oid;
    private String trade_id;
    private String maker_order_id;
    private String taken_order_id;
    private String order_type;

    public String getTaken_order_id() {
        return taken_order_id;
    }

    public void setTaken_order_id(String taken_order_id) {
        this.taken_order_id = taken_order_id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getRemaining_size() {
        return remaining_size;
    }

    public void setRemaining_size(String remaining_size) {
        this.remaining_size = remaining_size;
    }

    public String getProduct_id() {
        return product_id;
    }

    public void setProduct_id(String product_id) {
        this.product_id = product_id;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getClient_oid() {
        return client_oid;
    }

    public void setClient_oid(String client_oid) {
        this.client_oid = client_oid;
    }

    public String getTrade_id() {
        return trade_id;
    }

    public void setTrade_id(String trade_id) {
        this.trade_id = trade_id;
    }

    public String getMaker_order_id() {
        return maker_order_id;
    }

    public void setMaker_order_id(String maker_order_id) {
        this.maker_order_id = maker_order_id;
    }

    public String getOrder_type() {
        return order_type;
    }

    public void setOrder_type(String order_type) {
        this.order_type = order_type;
    }
}
