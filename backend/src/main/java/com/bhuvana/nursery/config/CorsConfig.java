package com.bhuvana.nursery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Cross-origin access for deployments where the React app and the API do not
 * share an origin (for example Netlify calling Render directly).
 *
 * <p>The recommended Netlify setup proxies {@code /api} to the backend, which
 * makes requests same-origin and leaves this list empty. When a split origin is
 * used, list the exact frontend origin(s) in {@code CORS_ALLOWED_ORIGINS};
 * wildcards are deliberately not supported because credentials (session and
 * CSRF cookies) are enabled. Never add {@code *} here.</p>
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.security.cors.allowed-origins:}") String allowedOrigins
    ) {
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (origins.isEmpty()) {
            // Same-origin deployment: no cross-origin requests are permitted.
            return source;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
