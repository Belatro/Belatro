package backend.belatro.context;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@Profile("test")
public class TestHttpSecurityPermitWs {

    @Bean
    SecurityFilterChain testWsChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/ws-native/**")      // only that endpoint
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}