package backend.belatro.context;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@TestConfiguration          // loads only during tests
public class TestWebSocketMessageSecurity
        extends AbstractSecurityWebSocketMessageBrokerConfigurer {

    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages.anyMessage().permitAll();   // disable message security in tests
    }

    @Override
    protected boolean sameOriginDisabled() { // tests don’t send an Origin header
        return true;
    }
}
