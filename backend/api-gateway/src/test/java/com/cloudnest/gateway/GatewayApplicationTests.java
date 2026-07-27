package com.cloudnest.gateway;

import com.cloudnest.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the API Gateway application context
 * and its core components.
 * <p>
 * Config Server and Eureka are disabled so tests run fully offline
 * without external dependencies.
 */
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=YnVnZmF0YWxzbm93bGVvcGFyZHN0cmV0Y2hpbmdvbmV0d29zbm93ZmFsbGluZ3RocmVlc2ZhbGxpbmc="
})
class GatewayApplicationTests {

    @Autowired(required = false)
    private JwtUtil jwtUtil;

    /**
     * Ensures the Spring application context loads without errors.
     */
    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertThat(jwtUtil).as("JwtUtil bean should be present in context").isNotNull();
    }

    // ── JWT Utility Tests (offline, no external services) ────────────────

    private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
            "testSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm".getBytes()
    );

    private JwtUtil testJwtUtil;
    private String validToken;

    @BeforeEach
    void setUp() {
        testJwtUtil = new JwtUtil(TEST_SECRET);

        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        validToken = Jwts.builder()
                .claims()
                    .add("userId", 1L)
                    .add("email", "test@cloudnest.com")
                    .add("role", "USER")
                    .and()
                .subject("test@cloudnest.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("JwtUtil validates a correctly signed token")
    void validateToken_withValidToken_returnsClaims() {
        Optional<io.jsonwebtoken.Claims> claims = testJwtUtil.validateToken(validToken);
        assertThat(claims).isPresent();
        assertThat(claims.get().get("email", String.class)).isEqualTo("test@cloudnest.com");
    }

    @Test
    @DisplayName("JwtUtil rejects an expired token")
    void validateToken_withExpiredToken_returnsEmpty() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        String expiredToken = Jwts.builder()
                .claims()
                    .add("userId", 1L)
                    .add("email", "test@cloudnest.com")
                    .and()
                .subject("test@cloudnest.com")
                .issuedAt(new Date(System.currentTimeMillis() - 3600_000))
                .expiration(new Date(System.currentTimeMillis() - 1))
                .signWith(key)
                .compact();

        Optional<io.jsonwebtoken.Claims> claims = testJwtUtil.validateToken(expiredToken);
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("JwtUtil rejects a malformed token")
    void validateToken_withMalformedToken_returnsEmpty() {
        Optional<io.jsonwebtoken.Claims> claims = testJwtUtil.validateToken("not.a.jwt");
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("JwtUtil rejects a token with wrong signature")
    void validateToken_withWrongSignature_returnsEmpty() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(
                        Base64.getEncoder().encodeToString("aDifferentKeyThatIsAlso256BitsLongForTesting".getBytes())
                )
        );
        String wrongToken = Jwts.builder()
                .subject("test@cloudnest.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(wrongKey)
                .compact();

        Optional<io.jsonwebtoken.Claims> claims = testJwtUtil.validateToken(wrongToken);
        assertThat(claims).isEmpty();
    }

    @Test
    @DisplayName("JwtUtil extracts claims from a valid token")
    void getUserId_and_getEmail() {
        Optional<io.jsonwebtoken.Claims> claims = testJwtUtil.validateToken(validToken);
        assertThat(claims).isPresent();

        assertThat(testJwtUtil.getUserId(claims.get())).hasValue(1L);
        assertThat(testJwtUtil.getEmail(claims.get())).hasValue("test@cloudnest.com");
        assertThat(testJwtUtil.getRole(claims.get())).hasValue("USER");
    }
}
