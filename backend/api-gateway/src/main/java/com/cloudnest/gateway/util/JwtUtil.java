package com.cloudnest.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility for validating and parsing JWT tokens issued by the Auth Service.
 * <p>
 * Uses the shared {@code jwt.secret} from the centralized configuration.
 * The secret is Base64-decoded to construct a {@link SecretKey} for HMAC-SHA
 * verification.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Validates the given JWT token and returns its claims if valid.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return an {@link Optional} containing the validated {@link Claims},
     *         or empty if the token is invalid or expired.
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);

            return Optional.of(jws.getPayload());
        } catch (SecurityException e) {
            log.warn("JWT signature mismatch: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // -- Convenience claim extractors ---------------------------------------

    /**
     * Extracts the {@code userId} claim (Long) from a validated token.
     */
    public Optional<Long> getUserId(Claims claims) {
        try {
            return Optional.ofNullable(claims.get("userId", Long.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the {@code email} claim (String) from a validated token.
     */
    public Optional<String> getEmail(Claims claims) {
        return Optional.ofNullable(claims.get("email", String.class));
    }

    /**
     * Extracts the {@code role} claim (String) from a validated token.
     */
    public Optional<String> getRole(Claims claims) {
        return Optional.ofNullable(claims.get("role", String.class));
    }
}
