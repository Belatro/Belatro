package backend.belatro;

import backend.belatro.dtos.BidMsg;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.Team;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end WebSocket ↔ Redis test without using @MockBean.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "logging.level.org.springframework.messaging.simp.stomp=TRACE",
                "spring.main.allow-circular-references=true",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@Testcontainers
class GameSocketControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry r) {
        r.add("spring.redis.host", redis::getHost);
        r.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort int port;

    @Autowired
    BelotGameService service;
    WebSocketStompClient stomp;

    @BeforeEach
    void setup() {
        SockJsClient sockJs = new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))
        );
        this.stomp = new WebSocketStompClient(sockJs);
        this.stomp.setMessageConverter(new MappingJackson2MessageConverter());

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.afterPropertiesSet();           // initialize the internal thread pool
        stomp.setTaskScheduler(scheduler);
        stomp.setReceiptTimeLimit(5_000);
    }

    @Test
    void bid_roundTrips_publicView_and_Redis() throws Exception {
        // 1) start & persist a new game
        String gameId = UUID.randomUUID().toString();
        service.start(
                gameId,
                new Team(List.of(new Player("Alice"), new Player("Carol"))),
                new Team(List.of(new Player("Bob"),   new Player("Dave"))));
        BelotGame game = service.get(gameId);      // fetch the freshly-started game
        String starter = game.getCurrentLead().getId();



        // 2) prepare your futures
        CompletableFuture<PublicGameView> pubF = new CompletableFuture<>();
        CompletableFuture<PrivateGameView> prvF   = new CompletableFuture<>();
        CompletableFuture<String> rawF = new CompletableFuture<>();

        // 3) create a handler that subscribes and immediately sends once connected
        StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() {
            @Override
            public void handleException( StompSession sess, StompCommand cmd,
                                         StompHeaders hdrs, byte[] payload, Throwable ex ) {
                ex.printStackTrace();
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {


                // ---- real DTO subscription ----
                session.subscribe(
                        "/topic/games/" + gameId,
                        new JsonFrameHandler(pubF, PublicGameView.class)
                );
                session.subscribe(
                        "/user/queue/games/" + gameId,
                        new JsonFrameHandler(prvF, PrivateGameView.class)
                );

                // now that we’re definitely subscribed, send the bid
                session.send(
                        "/app/games/" + gameId + "/bid",
                        new BidMsg(starter, false, Boja.HERC)
                );
            }
        };

        // 4) open STOMP-over-WebSocket with our session handler, supplying a "login" header
        String url = "ws://localhost:" + port + "/ws?user=" + starter;
    // (a) the low-level HTTP headers—usually empty for SockJS
        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();

    // (b) the STOMP CONNECT headers where we set the principal name
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("login", starter);   // e.g. "Alice"

    // now call the 4-arg connectAsync
        StompSession session = stomp
                .connectAsync(
                        url,
                        wsHeaders,
                        connectHeaders,
                        handler
                )
                .get(5, TimeUnit.SECONDS);

        // 5) now simply wait for the response
        PublicGameView pub = pubF.get(5, TimeUnit.SECONDS);
        PrivateGameView privateView = prvF.get(5, TimeUnit.SECONDS);

        // Now you can inspect your hand:
        System.out.println("My hand: " + privateView.hand());
        assertThat(privateView.hand()).isNotEmpty();
        // And you can still assert on public state:
        assertThat(pub.gameState()).isEqualTo(GameState.PLAYING);

        // 6) reload from Redis and assert persistence
        BelotGame reloaded = service.get(gameId);
        assertThat(reloaded.getBids()).hasSize(1);
//        assertThat(reloaded.getTrumpCaller().getId()).isEqualTo(game.getCurrentLead().getId());
    }

    /** Decode a JSON STOMP frame into a CompletableFuture */
    private static final class JsonFrameHandler implements StompFrameHandler {
        private final CompletableFuture<Object> future;
        private final Class<?> targetType;
        JsonFrameHandler(CompletableFuture<?> f, Class<?> t) {
            //noinspection unchecked
            this.future = (CompletableFuture<Object>) f;
            this.targetType = t;
        }
        @Override public @NotNull Type getPayloadType(StompHeaders h) { return targetType; }
        @Override public void handleFrame(StompHeaders h, Object p) { future.complete(p); }
    }

    /** Test‐only beans to disable security & mock out IMatchService */
    @TestConfiguration
    static class TestConfig {
        @Bean IMatchService matchService() {
            return Mockito.mock(IMatchService.class);
        }
        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        SecurityFilterChain websocketSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    // only apply this permissive chain to SockJS and STOMP endpoints:
                    .securityMatcher("/ws/**", "/app/**")
                    // disable CSRF so SockJS info and XHR frames work
                    .csrf(AbstractHttpConfigurer::disable)
                    // and allow absolutely everything on those paths
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

}
