package com.cloudnest.auth.security;

import com.cloudnest.auth.jwt.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Servlet filter that validates JWT Bearer tokens on every request
 * <em>except</em> public endpoints (registration, login, etc.).
 * <p>
 * On successful validation the {@link SecurityContextHolder} is populated
 * with a {@link UsernamePasswordAuthenticationToken} containing the user's
 * identity and role, so downstream Spring Security decisions
 * (e.g. {@code .anyRequest().authenticated()}) can proceed.
 * <p>
 * On missing or invalid tokens the request is terminated with
 * {@code 401 Unauthorized} and an empty body.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /** Pre-compiled matchers for public endpoints (same paths as {@link SecurityConstants}). */
    private final List<AntPathRequestMatcher> publicPathMatchers;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
        this.publicPathMatchers = Arrays.stream(SecurityConstants.PUBLIC_PATHS)
                .map(AntPathRequestMatcher::new)
                .toList();
    }

    /**
     * Returns {@code true} to skip filtering for public endpoints,
     * using the same Ant-style matching as {@code requestMatchers()} in
     * {@link SecurityConfig}.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicPathMatchers.stream()
                .anyMatch(matcher -> matcher.matches(request));
    }

    /**
     * Extracts, validates, and applies the JWT Bearer token.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // -- Extract the Authorization header --------------------------------
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {} {}", request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);

        // -- Validate the JWT ------------------------------------------------
        Optional<Claims> claimsOpt = jwtProvider.validateToken(token);

        if (claimsOpt.isEmpty()) {
            log.warn("Invalid or expired JWT for: {} {}", request.getMethod(), request.getRequestURI());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Invalid or expired token\"}");
            return;
        }

        Claims claims = claimsOpt.get();

        // -- Build the Spring Security authentication object -----------------
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities = (role != null)
                ? List.of(new SimpleGrantedAuthority(role))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        // Attach the full claims as details for downstream use
        authentication.setDetails(claims);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Authenticated user '{}' for: {} {}", username, request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }
}
