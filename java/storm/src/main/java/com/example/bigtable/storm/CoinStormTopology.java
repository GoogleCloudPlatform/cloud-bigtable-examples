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

package com.example.bigtable.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import com.example.bigtable.storm.bolts.BigtableBolt;
import com.example.bigtable.storm.spout.CoinbaseSpout;


/**
 * This is a topology that serves as an example of how to use Storm
 * with Google Cloud Bigtable. We read data from the Coinbase Market API
 * using the WebSocket stream, and store the values in Cloud Bigtable rows.
 */
public class CoinStormTopology {

    /**
     * Example usage, assuming Coinbase is a table name in Cloud Bigtable
     *
     * storm jar cloud-bigtable-coinstorm-1.0.0.jar com.example.bigtable.storm.CoinStormTopology Coinbase coinbase_topology
     *
     * @param args args[0] is the Bigtable table name, args[1] is the
     *             topology name. If there is no topology name it will use a
     *             local development cluster instead of submitting to a remote
     *             cluster using the topology name "test".
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

        // A very simple topology: our coinbase socket emits a stream of
        // CoinbaseData objects that the BigtableBolt puts into Cloud Bigtable
        builder.setSpout("coinbase", new CoinbaseSpout(), 1);
        builder.setBolt("bigtable", new BigtableBolt(args[0]), 1)
                .shuffleGrouping("coinbase");

        Config conf = new Config();
        conf.setDebug(true);

        if (args != null && args.length > 1) {
            conf.setNumWorkers(3);
            StormSubmitter.submitTopologyWithProgressBar(args[1], conf,
                    builder.createTopology());
        }
        else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("test", conf, builder.createTopology());
            Utils.sleep(20000);
            cluster.killTopology("test");
            cluster.shutdown();
        }
    }
}
