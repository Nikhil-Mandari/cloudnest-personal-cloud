package com.cloudnest.auth.security;

/**
 * Shared security constants used by both {@link SecurityConfig} and
 * {@link JwtAuthenticationFilter} to ensure public path matching is
 * consistent across the filter chain and authorization rules.
 */
public final class SecurityConstants {

    /**
     * Ant-style path patterns that are accessible without any authentication.
     * <p>
     * These are used by:
     * <ul>
     *   <li>{@link SecurityConfig#securityFilterChain} —
     *       {@code requestMatchers(PUBLIC_PATHS).permitAll()}</li>
     *   <li>{@link JwtAuthenticationFilter#shouldNotFilter} —
     *       to skip JWT validation for public endpoints</li>
     * </ul>
     */
    public static final String[] PUBLIC_PATHS = {
            "/api/auth/register",
            "/api/auth/register/verify",
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/login/verify",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/forgot-password/verify",
            "/api/auth/forgot-password/reset",
            "/api/auth/reset-password",
            "/api/auth/otp/resend",
            "/api/auth/otp-verification",
            "/api/auth/login/2fa",
            "/api/auth/passkeys/authenticate/start",
            "/api/auth/passkeys/authenticate/finish",
            "/api/auth/oauth/**",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/favicon.ico"
    };

    private SecurityConstants() {
        // prevent instantiation
    }
}
