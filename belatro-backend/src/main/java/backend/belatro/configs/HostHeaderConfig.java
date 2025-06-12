//package backend.belatro.configs;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.web.firewall.StrictHttpFirewall;
//import org.springframework.security.web.firewall.HttpFirewall;
//
//@Configuration
//public class HostHeaderConfig {
//
//    /**
//     * A firewall that will allow any Host header (127.0.0.1, localhost, foo.bar, etc.).
//     */
//    @Bean
//    public HttpFirewall allowAllHostsFirewall() {
//        StrictHttpFirewall fw = new StrictHttpFirewall();
//        // allow absolutely any host name
//        fw.setAllowedHostnames(hostname -> true);
//        return fw;
//    }
//
//    /**
//     * Tell Spring Security to use our custom firewall.
//     */
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall firewall) {
//        return web -> web.httpFirewall(firewall);
//    }
//}