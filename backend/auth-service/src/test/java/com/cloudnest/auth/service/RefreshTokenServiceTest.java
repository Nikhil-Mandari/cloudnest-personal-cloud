package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.entity.UserSession;
import com.cloudnest.auth.exception.InvalidRefreshTokenException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.repository.UserSessionRepository;
import com.cloudnest.auth.util.Hashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link RefreshTokenService} using a real {@link JwtProvider}
 * and mocked repositories (no Spring context).
 */
class RefreshTokenServiceTest {

    private static final long USER_ID = 7L;
    private static final String SESSION_ID = "session-123";

    private RefreshTokenRepository refreshTokenRepository;
    private UserSessionRepository sessionRepository;
    private UserCredentialRepository userRepository;
    private SessionService sessionService;
    private JwtProvider jwtProvider;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        sessionRepository = mock(UserSessionRepository.class);
        userRepository = mock(UserCredentialRepository.class);
        sessionService = mock(SessionService.class);
        jwtProvider = new JwtProvider("test-secret-for-unit-tests-only-0123456789abcdef", 900_000, 30, 10);
        service = new RefreshTokenService(refreshTokenRepository, sessionRepository,
                userRepository, jwtProvider, sessionService, new AuthProperties());
    }

    @Test
    @DisplayName("Issue stores only the SHA-256 hash of the refresh token")
    void issue_storesHashOnly() {
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.TokenPair pair = service.issue(USER_ID, "tester", "t@e.com", "ROLE_USER", SESSION_ID);

        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertNotEquals(pair.refreshToken(), Hashing.sha256Hex(pair.refreshToken()));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertEquals(Hashing.sha256Hex(pair.refreshToken()), stored.getTokenHash());
        assertEquals(SESSION_ID, stored.getSessionId());
    }

    @Test
    @DisplayName("Rotation revokes the old token and returns a fresh pair")
    void rotate_revokesOldToken() {
        String raw = jwtProvider.generateRefreshToken(USER_ID, SESSION_ID);

        RefreshToken stored = RefreshToken.builder()
                .id(1L)
                .userId(USER_ID)
                .sessionId(SESSION_ID)
                .tokenHash(Hashing.sha256Hex(raw))
                .jti("old-jti")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(Hashing.sha256Hex(raw)))
                .thenReturn(Optional.of(stored));
        when(sessionRepository.findBySessionIdAndActiveTrue(SESSION_ID))
                .thenReturn(Optional.of(UserSession.builder().sessionId(SESSION_ID).userId(USER_ID).active(true).build()));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(UserCredential.builder().id(USER_ID).username("tester").email("t@e.com").role("ROLE_USER").build()));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.revokeIfActive(any(), any())).thenReturn(1);

        RefreshTokenService.TokenPair pair = service.rotate(raw);

        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertNotEquals(raw, pair.refreshToken());
        assertTrue(Boolean.TRUE.equals(stored.getRevoked()));
        assertNotNull(stored.getRevokedAt());
        // The old token's replaced_by must point at the NEW token's jti.
        assertNotNull(stored.getReplacedBy());
        assertNotEquals("old-jti", stored.getReplacedBy());
        verify(sessionService).touch(SESSION_ID);
    }

    @Test
    @DisplayName("Rotation rejects a token that lost the atomic-revoke race (concurrent reuse)")
    void rotate_rejectsConcurrentReuse() {
        String raw = jwtProvider.generateRefreshToken(USER_ID, SESSION_ID);
        RefreshToken stored = RefreshToken.builder()
                .id(1L).userId(USER_ID).sessionId(SESSION_ID)
                .tokenHash(Hashing.sha256Hex(raw)).jti("jti")
                .expiresAt(LocalDateTime.now().plusDays(30)).revoked(false)
                .createdAt(LocalDateTime.now()).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(Hashing.sha256Hex(raw)))
                .thenReturn(Optional.of(stored));
        when(sessionRepository.findBySessionIdAndActiveTrue(SESSION_ID))
                .thenReturn(Optional.of(UserSession.builder().sessionId(SESSION_ID).userId(USER_ID).active(true).build()));
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(UserCredential.builder().id(USER_ID).username("tester").email("t@e.com").role("ROLE_USER").build()));
        // Another request already rotated the token → CAS returns 0.
        when(refreshTokenRepository.revokeIfActive(any(), any())).thenReturn(0);

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(raw));
    }

    @Test
    @DisplayName("Rotation rejects an already-revoked token")
    void rotate_rejectsRevoked() {
        String raw = jwtProvider.generateRefreshToken(USER_ID, SESSION_ID);
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(Hashing.sha256Hex(raw)))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(raw));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rotation rejects a token whose session has ended")
    void rotate_rejectsEndedSession() {
        String raw = jwtProvider.generateRefreshToken(USER_ID, SESSION_ID);
        RefreshToken stored = RefreshToken.builder()
                .id(1L).userId(USER_ID).sessionId(SESSION_ID)
                .tokenHash(Hashing.sha256Hex(raw)).jti("jti")
                .expiresAt(LocalDateTime.now().plusDays(30)).revoked(false)
                .createdAt(LocalDateTime.now()).build();
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(Hashing.sha256Hex(raw)))
                .thenReturn(Optional.of(stored));
        when(sessionRepository.findBySessionIdAndActiveTrue(SESSION_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(raw));
    }

    @Test
    @DisplayName("Rotation rejects a token that is not a refresh token")
    void rotate_rejectsAccessToken() {
        String access = jwtProvider.generateToken(USER_ID, "tester", "t@e.com", "ROLE_USER", SESSION_ID);
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(access));
    }

    @Test
    @DisplayName("Revoking by session marks every active token revoked")
    void revokeAllForSession() {
        RefreshToken token = RefreshToken.builder()
                .id(1L).userId(USER_ID).sessionId(SESSION_ID).tokenHash("hash").jti("jti")
                .expiresAt(LocalDateTime.now().plusDays(30)).revoked(false)
                .createdAt(LocalDateTime.now()).build();
        when(refreshTokenRepository.findBySessionId(SESSION_ID)).thenReturn(java.util.List.of(token));

        service.revokeAllForSession(SESSION_ID);

        assertTrue(Boolean.TRUE.equals(token.getRevoked()));
    }

    @Test
    @DisplayName("Rejects missing or malformed tokens")
    void rotate_rejectsMissing() {
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate(null));
        assertThrows(InvalidRefreshTokenException.class, () -> service.rotate("not-a-jwt"));
    }
}
