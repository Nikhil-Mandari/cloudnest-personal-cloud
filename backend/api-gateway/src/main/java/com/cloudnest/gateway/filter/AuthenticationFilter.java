package com.cloudnest.gateway.filter;

import com.cloudnest.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Global filter that authenticates requests via JWT before forwarding them
 * to downstream services.
 * <p>
 * <b>How it works:</b>
 * <ol>
 *   <li>Public endpoints (auth routes) are allowed through without a token.</li>
 *   <li>All other routes require a valid {@code Authorization: Bearer <token>} header.</li>
 *   <li>When a valid token is present, user identity ({@code X-User-Id},
 *       {@code X-User-Username}, {@code X-User-Email}, {@code X-User-Role})
 *       is forwarded as headers to the downstream service.</li>
 *   <li>Invalid / missing tokens on protected routes return {@code 401 Unauthorized}.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    /** Paths that do NOT require authentication. */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/register/verify",
            "/api/auth/login",
            "/api/auth/login/verify",
            "/api/auth/login/2fa",
            "/api/auth/passkeys/authenticate/start",
            "/api/auth/passkeys/authenticate/finish",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/forgot-password/verify",
            "/api/auth/forgot-password/reset",
            "/api/auth/reset-password",
            "/api/auth/otp/resend",
            "/api/auth/oauth/",
            "/api/shares/public/",
            "/actuator/",
            "/v3/api-docs",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/webjars/",
            "/favicon.ico"
    );

    /** Header through which the user ID is forwarded to downstream services. */
    private static final String USER_ID_HEADER = "X-User-Id";

    /** Header through which the username is forwarded to downstream services. */
    private static final String USER_USERNAME_HEADER = "X-User-Username";

    /** Header through which the user email is forwarded to downstream services. */
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    /** Header through which the user role is forwarded. */
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtUtil jwtUtil;

    /**
     * Applies authentication logic to every incoming request.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // -- Skip authentication for public endpoints -----------------------
        if (isPublicPath(path)) {
            log.debug("Skipping auth for public path: {}", path);
            return chain.filter(exchange);
        }

        // -- Extract the Authorization header --------------------------------
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", path);
            return unauthorized(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        // -- Validate the JWT ------------------------------------------------
        Optional<Claims> claimsOpt = jwtUtil.validateToken(token);

        if (claimsOpt.isEmpty()) {
            log.warn("Invalid or expired JWT for: {}", path);
            return unauthorized(exchange, "Invalid or expired token");
        }

        Claims claims = claimsOpt.get();

        // -- Forward user identity to downstream services --------------------
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(USER_ID_HEADER, jwtUtil.getUserId(claims).map(String::valueOf).orElse(""))
                .header(USER_USERNAME_HEADER, jwtUtil.getUsername(claims).orElse(""))
                .header(USER_EMAIL_HEADER, jwtUtil.getEmail(claims).orElse(""))
                .header(USER_ROLE_HEADER, jwtUtil.getRole(claims).orElse(""))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * Runs after the request logging filter so the log entry shows
     * the authentication outcome without unnecessary delay.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    // -- Private helpers -----------------------------------------------------

    /**
     * Returns {@code true} if the given request path matches a public endpoint.
     * <p>
     * Entries ending in {@code /} (directory prefixes such as
     * {@code /api/auth/oauth/}) match any path below them. Other entries are
     * matched exactly or at a path-segment boundary so that a public entry
     * like {@code /api/auth/login} can never mask a protected endpoint such as
     * {@code /api/auth/login-history}.
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(publicPath ->
                publicPath.endsWith("/")
                        ? path.startsWith(publicPath)
                        : path.equals(publicPath) || path.startsWith(publicPath + "/"));
    }

    /**
     * Terminates the request with {@code 401 Unauthorized} and the given message.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
