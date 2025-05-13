// src/main/java/backend/belatro/configs/WsConfig.java
package backend.belatro.configs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WsConfig implements WebSocketMessageBrokerConfigurer {


    private final TaskScheduler brokerTaskScheduler;

    @Autowired
    public WsConfig(TaskScheduler brokerTaskScheduler) {

        this.brokerTaskScheduler = brokerTaskScheduler;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry r) {
        // Browser opens WebSocket to http://…/ws
        r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry b) {
        b.setApplicationDestinationPrefixes("/app"); // client → server
        b.enableSimpleBroker("/topic")
                .setTaskScheduler(brokerTaskScheduler)
                .setHeartbeatValue(new long[] {10000, 10000})
                .enableDistributedBroker();
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
