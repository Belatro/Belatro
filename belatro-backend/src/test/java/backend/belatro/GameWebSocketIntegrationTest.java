package backend.belatro;

import backend.belatro.configs.RedisConfig;
import backend.belatro.pojo.gamelogic.BelotGame;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(
        classes = BelatroApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ImportAutoConfiguration(exclude = RedisConfig.class)
public class GameWebSocketIntegrationTest {

    @LocalServerPort        // ← inject the actual port
    int port;

    @Test
    public void testBidOverWebSocket() throws Exception {
        String gameId = "6813a774ece0383e80133fed";

        // 1) SockJS + STOMP client
        WebSocketStompClient stompClient = new WebSocketStompClient(
                new SockJsClient(Collections.singletonList(
                        new WebSocketTransport(new StandardWebSocketClient())
                ))
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        CountDownLatch latch = new CountDownLatch(1);

        // 2) Connect to the real port
        String url = "http://localhost:" + port + "/ws";
        StompSession session = stompClient
                        .connectAsync(url, new StompSessionHandlerAdapter() {}, new StompHeaders())
                        .get(1, TimeUnit.SECONDS);

        // 3) Subscribe
        session.subscribe("/topic/games/" + gameId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) {
                return BelotGame.class;
            }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                System.out.println("📣 Received game update: " + payload);
                latch.countDown();
            }
        });

        // 4) Send a bid
        Map<String,Object> payload = Map.of(
                "playerId", "67dc15dc8f766072dd839fac",
                "pass",     true,
                "trump",    "PIK"      // Jackson will accept null here
        );
        session.send("/app/games/" + gameId + "/bid", payload);

        // 5) Wait for update
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("No game update within 5s");
        }
        session.disconnect();
    }
}
