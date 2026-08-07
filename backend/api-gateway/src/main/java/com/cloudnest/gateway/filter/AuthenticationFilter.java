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
 *       {@code X-User-Email}) is forwarded as headers to the downstream service.</li>
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
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/otp/",
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
        // `set` REPLACES any caller-supplied identity headers instead of
        // appending, so a client can never spoof X-User-Id / X-User-Role: the
        // downstream service always sees the JWT-derived value first.
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(USER_ID_HEADER, jwtUtil.getUserId(claims).map(String::valueOf).orElse(""));
                    headers.set(USER_EMAIL_HEADER, jwtUtil.getEmail(claims).orElse(""));
                    headers.set(USER_ROLE_HEADER, jwtUtil.getRole(claims).orElse(""));
                })
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
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Terminates the request with {@code 401 Unauthorized} and the given message.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
