package com.cloudnest.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration for the API Gateway.
 * <p>
 * Uses the reactive {@link CorsWebFilter} (WebFlux-compatible) instead of
 * the Servlet-based {@code CorsFilter}. Allows the React frontend (or any
 * trusted origin) to call the Gateway from a browser.
 */
@Configuration
public class CorsConfig {

    // -- Allowed origins --------------------------------------------------
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",          // React dev server
            "http://localhost:5173",          // Vite dev server (alt)
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    );

    // -- HTTP methods exposed to the client --------------------------------
    private static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
    );

    // -- Headers the client may send ---------------------------------------
    private static final List<String> ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            // Stable device id sent by the frontend axios interceptor on every
            // request (OTP / trusted-device flows) — required for CORS preflight.
            "X-Device-Id",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
    );

    // -- Headers exposed to the client -------------------------------------
    private static final List<String> EXPOSED_HEADERS = List.of(
            "Authorization",
            "Content-Disposition"
    );

    /**
     * Reactive CORS filter applied to all paths.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setExposedHeaders(EXPOSED_HEADERS);
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
