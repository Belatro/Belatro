package backend.belatro.configs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
                                           Map<String, Object> attributes) {
                // parse ?user=Alice out of the URL
                String q = Optional.ofNullable(request.getURI().getQuery())
                        .orElse("");
                String user = Arrays.stream(q.split("&"))
                        .map(s -> s.split("="))
                        .filter(p -> p.length==2 && p[0].equals("user"))
                        .map(p -> p[1])
                        .findFirst()
                        .orElseThrow();
                attributes.put("user", user);
                return true;
            }
            @Override public void afterHandshake(ServerHttpRequest req,
                                                 ServerHttpResponse resp,
                                                 WebSocketHandler handler,
                                                 Exception ex) { }
        });

        // 2) now register your handshake handler
        reg.setHandshakeHandler(new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request,
                                              WebSocketHandler wsHandler,
                                              Map<String, Object> attributes) {
                // pull the “user” attribute you just stashed
                return () -> (String) attributes.get("user");
            }
        });

        // 3) only *then* enable SockJS
        reg.withSockJS();
    }
    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        // 1) Build an ObjectMapper with NO default typing
        ObjectMapper om = Jackson2ObjectMapperBuilder.json()
                .modules(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
                .build();
        om.deactivateDefaultTyping();    // critical!

        // 2) Register it as THE only JSON converter
        MappingJackson2MessageConverter mc = new MappingJackson2MessageConverter();
        mc.setObjectMapper(om);
        converters.clear();
        converters.add(mc);

        // returning true tells Spring to NOT register any others
        return true;
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
