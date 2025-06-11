package backend.belatro.context;          // use any test package you like

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Very small helper for WebSocket-STOMP tests.  Not production-grade
 * (no heart-beats / reconnects) – just enough for integration tests.
 */
public class StompTestClient implements AutoCloseable {

    private final WebSocketStompClient stomp;
    private final StompSession        session;

    /** Connect immediately to ws://… endpoint. */
    public StompTestClient(String wsUrl) throws Exception {
        stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter());

        CompletableFuture<StompSession> future = new CompletableFuture<>();
        stomp.connect(wsUrl, new StompSessionHandlerAdapter() {
            @Override public void afterConnected(StompSession s,
                                                 StompHeaders h) {
                future.complete(s);
            }
            @Override public void handleTransportError(StompSession s,
                                                       Throwable e) {
                future.completeExceptionally(e);
            }
        });
        session = future.get(3, TimeUnit.SECONDS);   // wait max 3 s
    }

    /** Subscribe and get a queue you can poll in assertions. */
    public <T> BlockingQueue<T> subscribe(String dest, Class<T> type) {
        BlockingQueue<T> q = new LinkedBlockingQueue<>();
        session.subscribe(dest, new StompFrameHandler() {
            public Type getPayloadType(StompHeaders headers) { return type; }
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders h, Object p) {
                q.add((T) p);
            }
        });
        return q;
    }

    @Override public void close() {
        if (session.isConnected()) session.disconnect();
        stomp.stop();
    }
}
