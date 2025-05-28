package backend.belatro.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonHttpConfig {

    /**
     * Customise the *primary* ObjectMapper used by Spring MVC:
     * – keep all Spring-Boot defaults
     * – explicitly deactivate default typing
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer mvcJacksonCustomizer() {
        return builder ->
                builder.postConfigurer(ObjectMapper::deactivateDefaultTyping); // λ solves the error
    }

}