/*
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.examples.coinflow.data.CoinbaseData;

import org.testng.annotations.Test;

public class CoinbaseSourceTest {

  String testMessage =
      "{\"type\":\"received\",\"sequence\":256819612,"
          + "\"order_id\":\"3cbbe784-6a57-4680-8080-e0a17fd98a65\","
          + "\"order_type\":\"limit\",\"size\":\"2.363\",\"price\":\"233"
          + ".24\","
          + "\"side\":\"buy\",\"funds\":null,\"product_id\":\"BTC-USD\","
          + "\"time\":\"2015-09-17T21:27:11.554604Z\"}";

  @Test
  public void deserializationTest() {
    CoinbaseSource.DeserializeCoinbase deserializeCoinbase =
        new CoinbaseSource.DeserializeCoinbase();
    CoinbaseData msg = deserializeCoinbase.deserializeData(testMessage);
    assertThat(msg.getType()).isEqualTo("received");
  }
}
