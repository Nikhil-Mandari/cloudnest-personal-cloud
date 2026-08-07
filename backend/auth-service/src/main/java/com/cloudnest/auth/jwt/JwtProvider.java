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
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Provider for JWT access, refresh and OTP-challenge token generation and
 * validation.
 * <p>
 * Reads {@code jwt.secret}, {@code jwt.expiration-ms} (access tokens) and
 * {@code auth.token.*} (refresh / challenge lifetimes) from the centralized
 * configuration (Config Server). The secret supports both Base64-encoded and
 * plain-text formats for flexibility.
 * <p>
 * Access tokens carry a {@code sid} (session id) and {@code jti} claim so
 * sessions can be ended and refresh tokens rotated/revoked independently.
 */
@Slf4j
@Component
public class JwtProvider {

    /** Claim holding the session id. */
    public static final String CLAIM_SESSION_ID = "sid";

    /** Claim holding the unique token id. */
    public static final String CLAIM_TOKEN_ID = "jti";

    /** Claim holding the token type (ACCESS / REFRESH / CHALLENGE). */
    public static final String CLAIM_TYPE = "type";

    /** Claim holding the OTP challenge purpose. */
    public static final String CLAIM_PURPOSE = "purpose";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final long challengeExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${auth.token.refresh-expiration-days:30}") int refreshExpirationDays,
            @Value("${auth.token.challenge-expiration-minutes:10}") int challengeExpirationMinutes) {
        this.signingKey = new SecretKeySpec(decodeSecret(secret), "HmacSHA256");
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationDays * 24L * 60L * 60L * 1000L;
        this.challengeExpirationMs = challengeExpirationMinutes * 60L * 1000L;
        log.info("JwtProvider initialized — access={}ms, refresh={}ms, challenge={}ms",
                expirationMs, this.refreshExpirationMs, this.challengeExpirationMs);
    }

    /**
     * Generates an access token for the given user (backward-compatible
     * signature — no session binding).
     */
    public String generateToken(Long userId, String username, String email, String role) {
        return generateToken(userId, username, email, role, null);
    }

    /**
     * Generates an access token bound to a session.
     *
     * @param sessionId the session id (may be {@code null} for legacy calls)
     */
    public String generateToken(Long userId, String username, String email, String role, String sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("role", role)
                .claim(CLAIM_TOKEN_ID, UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, "ACCESS")
                .claim(CLAIM_SESSION_ID, sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a rotating refresh token bound to a session.
     *
     * @return the raw refresh token (only the SHA-256 hash may be persisted)
     */
    public String generateRefreshToken(Long userId, String sessionId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim(CLAIM_TOKEN_ID, UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, "REFRESH")
                .claim(CLAIM_SESSION_ID, sessionId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a short-lived OTP challenge token used to carry the pending
     * login/registration/reset through the OTP verification step.
     */
    public String generateChallengeToken(Long userId, String purpose) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + challengeExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("userId", userId)
                .claim(CLAIM_TOKEN_ID, UUID.randomUUID().toString())
                .claim(CLAIM_TYPE, "CHALLENGE")
                .claim(CLAIM_PURPOSE, purpose)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    // -- Claims helpers -----------------------------------------------------

    /**
     * Extracts the {@code jti} claim (token id) from validated claims.
     */
    public String extractTokenId(Claims claims) {
        return claims.get(CLAIM_TOKEN_ID, String.class);
    }

    /**
     * Extracts the {@code sid} claim (session id) from validated claims.
     */
    public String extractSessionId(Claims claims) {
        return claims.get(CLAIM_SESSION_ID, String.class);
    }

    /**
     * Extracts the token {@code type} claim (ACCESS / REFRESH / CHALLENGE).
     */
    public String extractType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }

    /**
     * Returns the expiry instant of validated claims.
     */
    public Instant extractExpiry(Claims claims) {
        return claims.getExpiration().toInstant();
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
