package com.privacyfinance.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Global CORS configuration.
 *
 * <p>Allows the React Native Metro bundler (localhost:8081 / 19006) and
 * any device on the local Wi-Fi network to call our REST APIs.
 * On production we restrict origins further; here we keep it permissive
 * for local-network use only.
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:8081,http://localhost:19006}")
    private String[] allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow configured origins plus any local-network 192.168.x.x device
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://192.168.*.*:*",
                "http://10.*.*.*:*"
        ));

        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
