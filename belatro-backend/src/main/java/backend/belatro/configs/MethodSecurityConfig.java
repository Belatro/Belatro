package backend.belatro.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * ❷ Enables Spring Security’s @PreAuthorize / @PostAuthorize /
 *    @Secured / @RolesAllowed annotations application-wide.
 */
@Configuration
@EnableMethodSecurity   // Spring Security 6 / Boot 3
public class MethodSecurityConfig {
}