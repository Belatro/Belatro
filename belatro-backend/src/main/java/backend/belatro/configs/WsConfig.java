// src/main/java/backend/belatro/configs/WsConfig.java
package backend.belatro.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry r) {
        // Browser opens WebSocket to http://…/ws
        r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry b) {
        b.setApplicationDestinationPrefixes("/app"); // client → server
        b.enableSimpleBroker("/topic");              // server → client
    }
}
