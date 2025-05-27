package backend.belatro.configs;

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
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocketMessageBroker
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public WsConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // grab the StompWebSocketEndpointRegistration
        var reg = registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // 1) for SockJS transport, intercept the HTTP handshake
        reg.addInterceptors(new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String,Object> attributes) {

                // keep current query-param code
                String qp = Optional.ofNullable(request.getURI().getQuery()).orElse("");
                String user = Arrays.stream(qp.split("&"))
                        .map(s -> s.split("="))
                        .filter(p -> p.length == 2 && p[0].equals("user"))
                        .map(p -> p[1])
                        .findFirst()
                        // NEW: if ?user not present, fall back to header
                        .orElseGet(() ->
                                request.getHeaders()
                                        .getFirst("X-Player-Name"));

                String authHeader = request.getHeaders().getFirst("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);
                    if (jwtTokenProvider.validateToken(jwt)) {
                        user = jwtTokenProvider.getUsername(jwt); // overwrite/confirm user
                        Authentication auth =
                                new UsernamePasswordAuthenticationToken(user, null, List.of());
                        attributes.put("SPRING.AUTHENTICATION", auth);
                    } else {
                        return false; // invalid token → refuse handshake
                    }
                }


                if (user == null || user.isBlank()) {
                    return false;          // refuse handshake – no identity
                }
                attributes.put("user", user);
                return true;
            }
            @Override public void afterHandshake(ServerHttpRequest req,
                                                 ServerHttpResponse resp,
                                                 WebSocketHandler handler,
                                                 Exception ex) { }
        });

        reg.setHandshakeHandler(new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                /* If we stored an Authentication, re-use it, otherwise fall
                   back to the simple String-based Principal you already had. */
                Object maybeAuth = attributes.get("SPRING.AUTHENTICATION");
                if (maybeAuth instanceof Authentication auth) {
                    return auth;
                }
                return () -> (String) attributes.get("user");
            }
        });


        reg.withSockJS();
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
                        Object authAttr = accessor.getSessionAttributes()
                                .get("SPRING.AUTHENTICATION");
                        if (authAttr instanceof Authentication auth) {
                            accessor.setUser(auth);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                    /* 2) Fallback: read JWT from native STOMP header */
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

    @Bean
    public TaskScheduler brokerTaskScheduler() {
        ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
        ts.setPoolSize(1);
        ts.setThreadNamePrefix("ws-heartbeat-");
        ts.initialize();
        return ts;
    }

}
