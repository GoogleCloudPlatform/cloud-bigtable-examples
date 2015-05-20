package com.example.bigtable.storm;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.example.bigtable.storm.data.CoinbaseData;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * This is currently just used for testing the socket, it isn't actually
 * part of the Storm topology.
 */
public class CoinbaseClient {


    public static void main(String[] args) {

        String destUri = "wss://ws-feed.exchange.coinbase.com";
        if (args.length > 0) {
            destUri = args[0];
        }
        WebSocketClient client = new WebSocketClient(new SslContextFactory());
        Queue<CoinbaseData> queue = new LinkedBlockingQueue<CoinbaseData>(100);
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
        } finally {
            try {
                client.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
