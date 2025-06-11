package backend.belatro.Integration;

import backend.belatro.context.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)

public class WebSocketNativeIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private List<String> messages = new CopyOnWriteArrayList<>();

    @BeforeEach
    public void setup() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter());
    }

    @Test
    public void testNativeEndpoint() throws Exception {
        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws-native",
                        new StompSessionHandlerAdapter() {})
                .get(1, TimeUnit.SECONDS);

        session.subscribe("/topic/test", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((String) payload);
            }
        });

        session.send("/app/echo", "ping");

        // wait up to 2s for a reply
        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .until(() -> messages.contains("ping"));

        assertTrue(messages.contains("ping"));
    }
}
