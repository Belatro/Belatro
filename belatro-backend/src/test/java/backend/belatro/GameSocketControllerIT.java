package backend.belatro;

import backend.belatro.dtos.BidMsg;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.pojo.gamelogic.*;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.services.BelotGameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GameSocketControllerIT {

    /* ── Redis container ────────────────────────────────────────────── */
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry reg) {
        reg.add("spring.redis.host", redis::getHost);
        reg.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    /* ── autopopulated test helpers ─────────────────────────────────── */
    @LocalServerPort
    int port;

    @Autowired
    BelotGameService service;

    @Autowired
    ObjectMapper json;                       // reuse app’s mapper

    WebSocketStompClient stomp;

    @BeforeEach
    void bootClient() {
        SockJsClient sockJs = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient())));
        stomp = new WebSocketStompClient(sockJs);
        stomp.setMessageConverter(new MappingJackson2MessageConverter());
    }

    /* ── the integration test ───────────────────────────────────────── */

    @Test
    void bid_roundTrips_throughWebSocket_andRedis() throws Exception {

        /* 1 ▸ start a fresh game and persist it ---------------------- */
        String gameId = UUID.randomUUID().toString();
        BelotGame g0 = service.start(
                gameId,
                new Team(List.of(new Player("Alice"), new Player("Carol"))),
                new Team(List.of(new Player("Bob"),   new Player("Dave"))));

        /* 2 ▸ connect as “Alice” over WebSocket ---------------------- */
        String url = "ws://localhost:" + port + "/ws";
        StompSession session = stomp
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        /* prepare latches / futures */
        CompletableFuture<PublicGameView>  pubF  = new CompletableFuture<>();
        CompletableFuture<PrivateGameView> privF = new CompletableFuture<>();

        session.subscribe("/topic/games/" + gameId, new JsonHandler(pubF, PublicGameView.class));
        session.subscribe("/user/queue/games/" + gameId, new JsonHandler(privF, PrivateGameView.class));

        /* 3 ▸ send a bid message ------------------------------------ */
        BidMsg bid = new BidMsg("Alice", false, Boja.HERC);
        session.send("/app/games/" + gameId + "/bid", bid);

        /* 4 ▸ await broadcasts -------------------------------------- */
        PublicGameView  pub  = pubF.get(3, TimeUnit.SECONDS);
        PrivateGameView priv = privF.get(3, TimeUnit.SECONDS);

        /* 5 ▸ assertions -------------------------------------------- */
        assertEquals(gameId, pub.gameId());
        assertEquals(GameState.BIDDING, pub.gameState());        // still bidding after first call-trump
        assertThat(pub.bids()).isNotEmpty();                     // call-trump recorded
        assertThat(priv.hand()).hasSize(6);                      // Alice still has 6 cards
        assertThat(priv.yourTurn()).isFalse();                   // turn passed to next player

        /* 6 ▸ re-load from Redis ------------------------------------ */
        BelotGame persisted = service.get(gameId);
        assertThat(persisted.getBids()).hasSize(1);
        assertThat(persisted.getTrumpCaller().getId()).isEqualTo("Alice");
    }

    /* ── small helper to decode frames ------------------------------ */
    private class JsonHandler implements StompFrameHandler {
        private final CompletableFuture<Object> sink;
        private final Class<?> type;
        JsonHandler(CompletableFuture<?> sink, Class<?> type) {
            //noinspection unchecked
            this.sink = (CompletableFuture<Object>) sink;
            this.type = type;
        }
        @Override public Type getPayloadType(StompHeaders headers) { return type; }
        @Override public void handleFrame(StompHeaders h, Object p) { sink.complete(p); }
    }
}
