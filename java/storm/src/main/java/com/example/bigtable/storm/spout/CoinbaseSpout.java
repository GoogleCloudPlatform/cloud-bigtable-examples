package com.example.bigtable.storm.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import com.example.bigtable.storm.CoinbaseSocket;
import com.example.bigtable.storm.data.CoinbaseData;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CoinbaseSpout extends BaseRichSpout {

    SpoutOutputCollector _collector;
    LinkedBlockingQueue<CoinbaseData> queue = null;

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("coin"));
    }

    @Override
    public void open(Map map, TopologyContext topologyContext,
                     SpoutOutputCollector collector) {
        queue = new LinkedBlockingQueue<>(1000);
        _collector = collector;

        String destUri = "wss://ws-feed.exchange.coinbase.com";
        WebSocketClient client = new WebSocketClient(new SslContextFactory());
        CoinbaseSocket socket = new CoinbaseSocket(queue);
        try {
            client.start();
            URI echoUri = new URI(destUri);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(socket, echoUri, request);
            System.out.printf("Connecting to : %s%n", echoUri);
            socket.awaitClose(50, TimeUnit.SECONDS);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void nextTuple() {
        CoinbaseData data = queue.poll();
        _collector.emit(new Values(data));
    }
}
