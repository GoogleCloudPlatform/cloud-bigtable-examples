package com.example.bigtable.storm;

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

@WebSocket(maxTextMessageSize = 64 * 1024)
public class CoinbaseSocket {

    private static final Logger logger = LoggerFactory.getLogger(CoinbaseSocket
            .class);

    private final CountDownLatch closeLatch;

    @SuppressWarnings("unused")
    private Session session;

    String send = "{\n" +
            "    \"type\": \"subscribe\",\n" +
            "    \"product_id\": \"BTC-USD\"\n" +
            "}";

    private Queue queue;

    private ObjectMapper objectMapper = new ObjectMapper();
    public CoinbaseSocket(Queue<CoinbaseData> queue) {
        this.closeLatch = new CountDownLatch(1);
        this.queue = queue;
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
        }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown();
    }

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

    @OnWebSocketMessage
    public void onMessage(String msg) {
        try {
            CoinbaseData data = objectMapper.readValue(msg, CoinbaseData
                    .class);
            queue.offer(data);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("bad data was: " + msg);
        }

        logger.info("Got msg: " +  msg);
    }
}
