package com.example.bigtable.storm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.utils.Utils;
import com.example.bigtable.storm.bolts.BigtableBolt;
import com.example.bigtable.storm.spout.CoinbaseSpout;

public class BigtableTopology {

    public static void main(String[] args) throws Exception {
        TopologyBuilder builder = new TopologyBuilder();

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
            Utils.sleep(10000);
            cluster.killTopology("test");
            cluster.shutdown();
        }
    }
}
