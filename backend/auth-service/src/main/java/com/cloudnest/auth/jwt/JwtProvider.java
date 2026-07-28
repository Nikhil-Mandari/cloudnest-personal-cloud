package com.cloudnest.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Provider for JWT token generation and validation.
 * <p>
 * Reads {@code jwt.secret} and {@code jwt.expiration-ms} from the
 * centralized configuration (Config Server). The secret supports both
 * Base64-encoded and plain-text formats for flexibility.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = new SecretKeySpec(decodeSecret(secret), "HmacSHA256");
        this.expirationMs = expirationMs;
        log.info("JwtProvider initialized with expiration-ms={}", expirationMs);
    }

    /**
     * Generates a JWT for the given user details.
     *
     * @param userId   the user's unique identifier
     * @param username the user's username
     * @param email    the user's email address
     * @param role     the user's role (e.g. {@code ROLE_USER})
     * @return a signed JWT string
     */
    public String generateToken(Long userId, String username, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates a JWT token and returns its claims if valid.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return an {@link Optional} containing the claims, or empty if invalid
     */
    public Optional<Claims> validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
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

    // -- Private helpers -----------------------------------------------------

    /**
     * Decodes the JWT secret: tries Base64 first, then falls back to
     * SHA-256 key derivation for plain-text values.
     */
    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            log.debug("JWT secret decoded from Base64");
            return decoded;
        } catch (IllegalArgumentException e) {
            log.warn("JWT secret is not valid Base64; falling back to SHA-256 key derivation");
            return sha256(secret);
        }
    }

    /**
     * Derives a 256-bit key from the given plain-text string using SHA-256.
     */
    private static byte[] sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
