package com.tukan.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the explicit CORS policy for the API (Sprint 6.3).
 *
 * <p>The allowed origins are externalized through {@code tukan.cors.allowed-origins}
 * (comma-separated), following the same property/profile pattern already used across
 * {@code application-*.properties}. The default targets the local Vite dev server so the
 * local frontend works out of the box; production overrides it via the
 * {@code CORS_ALLOWED_ORIGINS} environment variable in {@code application-prod.properties}.
 *
 * <p>Authentication is stateless (Bearer JWT in the {@code Authorization} header, never
 * cookies), so {@code allowCredentials} is intentionally {@code false}. This keeps the
 * policy safe: specific origins are listed and no credentialed wildcard is ever used.
 *
 * <p>The bean is named {@code corsConfigurationSource} so Spring Security's
 * {@code http.cors(...)} in {@link com.tukan.api.security.SecurityConfig} picks it up
 * automatically and applies it to the security filter chain (preflight included).
 */
@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type");

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${tukan.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
            String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
