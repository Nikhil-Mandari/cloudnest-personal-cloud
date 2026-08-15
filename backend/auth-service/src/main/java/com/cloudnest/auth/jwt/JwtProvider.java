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
        this.signingKey = new SecretKeySpec(decodeSecret(requireStrongSecret(secret)), "HmacSHA256");
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
        return generateTokenWithExpiry(userId, username, email, role, expirationMs, null);
    }

    /**
     * Generates a JWT with a custom expiry and an optional token-type claim
     * (e.g. {@code PASSWORD_RESET}).
     *
     * @param userId     the user's unique identifier
     * @param username   the user's username
     * @param email      the user's email address
     * @param role       the user's role
     * @param expiryMs   custom lifetime in milliseconds
     * @param tokenType  optional claim value for {@code type} (may be null)
     * @return a signed JWT string
     */
    public String generateTokenWithExpiry(Long userId, String username, String email, String role,
                                          long expiryMs, String tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry);

        if (tokenType != null) {
            builder.claim("type", tokenType);
        }

        return builder.signWith(signingKey).compact();
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
     * Fails fast at startup when the JWT secret is missing or weaker than
     * 256 bits. The shared config deliberately ships without a fallback
     * secret, so a missing {@code JWT_SECRET} must stop the service instead
     * of silently signing tokens with a known default.
     */
    private static String requireStrongSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable "
                            + "(Base64-encoded, at least 32 bytes) before starting the service.");
        }
        return secret;
    }

    /**
     * Decodes the JWT secret: tries Base64 first, then falls back to
     * SHA-256 key derivation for plain-text values. Secrets that resolve to
     * fewer than 32 bytes are rejected so a weak key never reaches the signer.
     */
    private static byte[] decodeSecret(String secret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            if (decoded.length < 32) {
                throw new IllegalStateException(
                        "JWT secret is too weak: Base64 value decodes to " + decoded.length
                                + " bytes. Use at least 32 bytes (256 bits).");
            }
            log.debug("JWT secret decoded from Base64");
            return decoded;
        } catch (IllegalArgumentException e) {
            if (secret.length() < 32) {
                throw new IllegalStateException(
                        "JWT secret is too weak: expected a Base64 value of at least 32 bytes, "
                                + "or a plain-text secret of at least 32 characters.");
            }
            log.warn("JWT secret is not valid Base64; deriving a 256-bit key via SHA-256 "
                    + "(set a Base64 JWT_SECRET for production)");
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
