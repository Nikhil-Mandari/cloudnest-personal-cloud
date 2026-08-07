package com.cloudnest.gateway.filter;

import com.cloudnest.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthenticationFilter}.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>All public endpoints bypass JWT authentication</li>
 *   <li>Protected endpoints reject requests with missing / invalid tokens</li>
 *   <li>Protected endpoints forward user identity headers on valid JWT</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationFilter")
class AuthenticationFilterTest {

    // -- Mocks -----------------------------------------------------------------

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private Claims claims;

    // -- Object under test -----------------------------------------------------

    private AuthenticationFilter filter;

    // -- Set-up -----------------------------------------------------------------

    @BeforeEach
    void setUp() {
        filter = new AuthenticationFilter(jwtUtil);

        // Common stubs — lenient so unused stubs do not interfere with
        // strict stubbing in tests that only exercise the public-path branch.
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(response.setComplete()).thenReturn(Mono.empty());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PUBLIC ENDPOINTS — every path in PUBLIC_PATHS must bypass auth
    // ══════════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "{0} should be allowed without a token")
    @ValueSource(strings = {
            "/api/auth/register",
            "/api/auth/register/verify",
            "/api/auth/login",
            "/api/auth/login/verify",
            "/api/auth/refresh",
            "/api/auth/forgot-password",
            "/api/auth/forgot-password/verify",
            "/api/auth/forgot-password/reset",
            "/api/auth/otp/resend"
    })
    @DisplayName("Auth public endpoints are allowed without Authorization header")
    void authPublicEndpoints_allowedWithoutToken(String path) {
        when(request.getURI()).thenReturn(URI.create(path));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Must proceed down the filter chain
        verify(chain).filter(exchange);
        // Must NOT perform JWT validation
        verify(jwtUtil, never()).validateToken(anyString());
        // Must NOT set any error status
        verify(response, never()).setStatusCode(any());
    }

    @ParameterizedTest(name = "{0} should be allowed without a token")
    @ValueSource(strings = {
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics"
    })
    @DisplayName("Actuator endpoints are allowed without Authorization header")
    void actuatorEndpoints_allowedWithoutToken(String path) {
        when(request.getURI()).thenReturn(URI.create(path));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("GET /v3/api-docs is allowed without Authorization header")
    void apiDocsEndpoint_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/v3/api-docs"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("GET /v3/api-docs/swagger-config is allowed without token")
    void apiDocsSubpath_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/v3/api-docs/swagger-config"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("GET /swagger-ui/index.html is allowed without Authorization header")
    void swaggerUiEndpoint_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/swagger-ui/index.html"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("GET /swagger-ui.html is allowed without Authorization header")
    void swaggerUiHtmlEndpoint_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/swagger-ui.html"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("GET /webjars/bootstrap/js/bootstrap.min.js is allowed without Authorization header")
    void webjarsEndpoint_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/webjars/bootstrap/js/bootstrap.min.js"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("GET /favicon.ico is allowed without Authorization header")
    void faviconEndpoint_allowedWithoutToken() {
        when(request.getURI()).thenReturn(URI.create("/favicon.ico"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROTECTED ENDPOINTS — must require a valid JWT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Protected endpoint returns 401 when Authorization header is missing")
    void protectedEndpoint_withoutToken_returnsUnauthorized() {
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response).setComplete();
        verify(chain, never()).filter(any());
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Protected endpoint returns 401 when Authorization header uses wrong scheme (not Bearer)")
    void protectedEndpoint_withWrongAuthScheme_returnsUnauthorized() {
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
        // JWT validation should NOT be attempted for non-Bearer auth
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("Protected endpoint returns 401 when Authorization header is empty Bearer")
    void protectedEndpoint_withEmptyBearer_returnsUnauthorized() {
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer ");
        when(request.getHeaders()).thenReturn(headers);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Protected endpoint returns 401 when JWT is invalid or expired")
    void protectedEndpoint_withInvalidToken_returnsUnauthorized() {
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");
        when(request.getHeaders()).thenReturn(headers);
        when(jwtUtil.validateToken("invalid-token")).thenReturn(Optional.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("Protected endpoint forwards request with JWT-derived identity headers when JWT is valid")
    void protectedEndpoint_withValidToken_forwardsWithUserHeaders() {
        // -- Arrange: request path and headers --
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        when(request.getHeaders()).thenReturn(headers);

        // -- Arrange: JWT validation succeeds --
        when(jwtUtil.validateToken("valid-token")).thenReturn(Optional.of(claims));
        when(jwtUtil.getUserId(claims)).thenReturn(Optional.of(1L));
        when(jwtUtil.getEmail(claims)).thenReturn(Optional.of("user@cloudnest.com"));
        when(jwtUtil.getRole(claims)).thenReturn(Optional.of("ROLE_USER"));

        // -- Arrange: request mutation --
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);

        // Capture the headers consumer so the actual forwarded values can be asserted.
        ArgumentCaptor<Consumer<HttpHeaders>> headersConsumer =
                ArgumentCaptor.forClass(Consumer.class);
        when(requestBuilder.headers(headersConsumer.capture())).thenReturn(requestBuilder);

        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        // -- Arrange: exchange mutation --
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        // -- Arrange: chain continues --
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // -- Act & Assert --
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(exchange);
        verify(response, never()).setStatusCode(any());

        // -- Assert the forwarded identity headers are derived from the JWT --
        HttpHeaders forwarded = new HttpHeaders();
        headersConsumer.getValue().accept(forwarded);
        assertThat(forwarded.getFirst("X-User-Id")).isEqualTo("1");
        assertThat(forwarded.getFirst("X-User-Email")).isEqualTo("user@cloudnest.com");
        assertThat(forwarded.getFirst("X-User-Role")).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Protected endpoint overwrites caller-supplied identity headers with JWT values")
    void protectedEndpoint_withSpoofedIdentityHeaders_forwardsJwtDerivedValues() {
        // -- Arrange: request carries spoofed identity headers --
        when(request.getURI()).thenReturn(URI.create("/api/users/me"));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        headers.add("X-User-Id", "999");
        headers.add("X-User-Role", "ROLE_ADMIN");
        when(request.getHeaders()).thenReturn(headers);

        when(jwtUtil.validateToken("valid-token")).thenReturn(Optional.of(claims));
        when(jwtUtil.getUserId(claims)).thenReturn(Optional.of(7L));
        when(jwtUtil.getEmail(claims)).thenReturn(Optional.of("real@cloudnest.com"));
        when(jwtUtil.getRole(claims)).thenReturn(Optional.of("ROLE_USER"));

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        ArgumentCaptor<Consumer<HttpHeaders>> headersConsumer =
                ArgumentCaptor.forClass(Consumer.class);
        when(requestBuilder.headers(headersConsumer.capture())).thenReturn(requestBuilder);

        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // The downstream service must see the JWT values, never the spoofed ones.
        HttpHeaders forwarded = new HttpHeaders();
        headersConsumer.getValue().accept(forwarded);
        assertThat(forwarded.getFirst("X-User-Id")).isEqualTo("7");
        assertThat(forwarded.getFirst("X-User-Email")).isEqualTo("real@cloudnest.com");
        assertThat(forwarded.getFirst("X-User-Role")).isEqualTo("ROLE_USER");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FILTER ORDER
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Filter runs at HIGHEST_PRECEDENCE + 1 (after RequestLoggingFilter)")
    void filterOrder_isCorrect() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
