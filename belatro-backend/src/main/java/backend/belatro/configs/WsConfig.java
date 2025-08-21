package backend.belatro.configs;

import backend.belatro.components.RateLimitingHandshakeInterceptor;
import backend.belatro.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.*;

@Configuration
@EnableWebSocketMessageBroker
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimitingHandshakeInterceptor rateLimitingHandshakeInterceptor;

    @Autowired
    public WsConfig(JwtTokenProvider jwtTokenProvider, RateLimitingHandshakeInterceptor rateLimitingHandshakeInterceptor) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.rateLimitingHandshakeInterceptor = rateLimitingHandshakeInterceptor;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // grab the StompWebSocketEndpointRegistration


        HandshakeInterceptor authInterceptor = new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest req,
                                           ServerHttpResponse resp,
                                           WebSocketHandler handler,
                                           Map<String, Object> attrs) {

                String qp = Optional.ofNullable(req.getURI().getQuery()).orElse("");
                String user = Arrays.stream(qp.split("&"))
                        .map(s -> s.split("="))
                        .filter(p -> p.length == 2 && p[0].equals("user"))
                        .map(p -> p[1])
                        .findFirst()
                        .orElseGet(() -> req.getHeaders().getFirst("X-Player-Name"));

                String raw = req.getHeaders().getFirst("Authorization");
                if (raw != null && raw.startsWith("Bearer ")) {
                    String jwt = raw.substring(7);
                    if (!jwtTokenProvider.validateToken(jwt)) return false;
                    user = jwtTokenProvider.getUsername(jwt);
                    Authentication auth =
                            new UsernamePasswordAuthenticationToken(user, null, List.of());
                    attrs.put("SPRING.AUTHENTICATION", auth);
                }
                if (user == null || user.isBlank()) return false;
                attrs.put("user", user);
                return true;
            }
            @Override public void afterHandshake(ServerHttpRequest r, ServerHttpResponse s,
                                                 WebSocketHandler h, Exception ex) { }
        };

        DefaultHandshakeHandler principalHandler = new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest req,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attrs) {
                Object maybe = attrs.get("SPRING.AUTHENTICATION");
                if (maybe instanceof Authentication a) return a;
                return () -> (String) attrs.get("user");
            }
        };
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(rateLimitingHandshakeInterceptor, authInterceptor)
                .setHandshakeHandler(principalHandler)
                .withSockJS()
                .setDisconnectDelay(30_000L);

        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(rateLimitingHandshakeInterceptor, authInterceptor)
                .setHandshakeHandler(principalHandler);

    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel ch) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    /* 1) First try to re-use what the handshake stored */
                    if (accessor.getUser() == null) {
                        Object authAttr = Objects.requireNonNull(accessor.getSessionAttributes())
                                .get("SPRING.AUTHENTICATION");
                        if (authAttr instanceof Authentication auth) {
                            accessor.setUser(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                    if (accessor.getUser() == null) {
                        String raw = accessor.getFirstNativeHeader("Authorization");
                        if (raw != null && raw.startsWith("Bearer ")) {
                            String jwt = raw.substring(7);
                            if (jwtTokenProvider.validateToken(jwt)) {
                                String username = jwtTokenProvider.getUsername(jwt);
                                Authentication auth =
                                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                                accessor.setUser(auth);
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            }
                        }
                    }
                }
                return message;
            }
        });
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");

        // enable both /topic and /queue
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(brokerTaskScheduler())
                .setHeartbeatValue(new long[]{10_000, 10_000});

        // tell Spring that “/user” is the prefix for user-targeted destinations
        registry.setUserDestinationPrefix("/user");
    }
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                .setMessageSizeLimit(64 * 1024)      // 64 KiB per STOMP frame
                .setSendBufferSizeLimit(512 * 1024)  // 512 KiB aggregated
                .setSendTimeLimit(15_000);           // 15 s max to flush buffer
    }


    @Bean
    public TaskScheduler brokerTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix("ws-heartbeat-");
        ts.initialize();
        return ts;
    }

}
