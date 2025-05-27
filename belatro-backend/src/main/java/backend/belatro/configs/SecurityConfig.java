package backend.belatro.configs;

import backend.belatro.security.JwtAuthenticationFilter;
import backend.belatro.services.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.config.Customizer;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    public static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);


    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean @Order(1)
    SecurityFilterChain publicAuthChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean @Order(2)
    SecurityFilterChain apiChain(HttpSecurity http,
                                 DaoAuthenticationProvider authProvider,
                                 HttpFirewall firewall) throws Exception {
        // force your custom firewall
        http.setSharedObject(HttpFirewall.class, firewall);

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/ws/**","/index.html")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * Override the stock firewall and allow any Host header.
     */
    @Bean
    @Primary
    public HttpFirewall allowAllHostsFirewall() {
        // Use StrictHttpFirewall but disable host-name validation completely
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHostnames(host -> true);   // <- allow any hostname or IP
        return firewall;
    }



    /**
     * Make sure Spring Security actually picks up our firewall.
     */
    @Bean
    @Order(0)
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
        return web -> web.httpFirewall(firewall);
    }

}