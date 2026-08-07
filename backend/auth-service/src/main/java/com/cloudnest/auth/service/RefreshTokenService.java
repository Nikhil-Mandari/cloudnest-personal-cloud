package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.InvalidRefreshTokenException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.repository.UserSessionRepository;
import com.cloudnest.auth.util.Hashing;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Manages the refresh-token lifecycle: issuance, rotation (each refresh
 * invalidates the previous token), and revocation (per token, per session,
 * or per user). Only SHA-256 hashes of tokens are persisted.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository sessionRepository;
    private final UserCredentialRepository userRepository;
    private final JwtProvider jwtProvider;
    private final SessionService sessionService;
    private final AuthProperties properties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserSessionRepository sessionRepository,
                               UserCredentialRepository userRepository,
                               JwtProvider jwtProvider,
                               SessionService sessionService,
                               AuthProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
        this.sessionService = sessionService;
        this.properties = properties;
    }

    /**
     * A freshly issued token pair.
     *
     * @param accessToken  short-lived access token
     * @param refreshToken raw refresh token (client-held)
     */
    public record TokenPair(String accessToken, String refreshToken) {
    }

    /**
     * Issues a new refresh token for the user + session and returns a token
     * pair (the access token is issued together).
     */
    @Transactional
    public TokenPair issue(Long userId, String username, String email, String role, String sessionId) {
        String raw = jwtProvider.generateRefreshToken(userId, sessionId);
        persist(userId, sessionId, raw, null);
        String access = jwtProvider.generateToken(userId, username, email, role, sessionId);
        return new TokenPair(access, raw);
    }

    /**
     * Rotates a refresh token: validates it, resolves the owning user from its
     * claims, revokes the old token and issues a fresh pair bound to the same
     * session. Reuse of a revoked token is rejected, and the session must still
     * be active (logging out a device revokes its tokens even on replay).
     *
     * @param rawToken the client-held refresh token
     * @return a new token pair
     */
    @Transactional
    public TokenPair rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }

        Claims claims = jwtProvider.validateToken(rawToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh token"));

        if (!"REFRESH".equals(jwtProvider.extractType(claims))) {
            throw new InvalidRefreshTokenException("Not a refresh token");
        }

        Long userId = claims.get("userId", Long.class);
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));

        String sessionId = jwtProvider.extractSessionId(claims);
        String tokenHash = Hashing.sha256Hex(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token has been revoked"));

        // The session must still be active — logging out of a device revokes
        // its refresh tokens even if the raw token is replayed.
        sessionRepository.findBySessionIdAndActiveTrue(sessionId)
                .orElseThrow(() -> new InvalidRefreshTokenException("Session is no longer active"));

        // Atomically revoke the old token. If another request already rotated
        // it (concurrent reuse), this call loses the race and is rejected.
        int revoked = refreshTokenRepository.revokeIfActive(stored.getId(), LocalDateTime.now());
        if (revoked == 0) {
            log.warn("Refresh token reuse detected for session {}", sessionId);
            throw new InvalidRefreshTokenException("Refresh token has already been used");
        }

        // Record the replacement chain: old token points to the new token's jti.
        String newRaw = jwtProvider.generateRefreshToken(userId, sessionId);
        String newJti = jwtProvider.extractTokenId(jwtProvider.validateToken(newRaw).orElseThrow());
        persist(userId, sessionId, newRaw, stored.getJti());
        stored.setRevoked(true);
        stored.setRevokedAt(LocalDateTime.now());
        stored.setReplacedBy(newJti);
        stored.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(stored);

        String access = jwtProvider.generateToken(userId, user.getUsername(),
                user.getEmail(), user.getRole(), sessionId);
        sessionService.touch(sessionId);
        log.debug("Refresh token rotated for session {}", sessionId);
        return new TokenPair(access, newRaw);
    }

    /**
     * Revokes a specific refresh token.
     */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedFalse(Hashing.sha256Hex(rawToken))
                .ifPresent(this::revoke);
    }

    /**
     * Revokes every active refresh token bound to a session.
     */
    @Transactional
    public void revokeAllForSession(String sessionId) {
        refreshTokenRepository.findBySessionId(sessionId)
                .stream()
                .filter(t -> !Boolean.TRUE.equals(t.getRevoked()))
                .forEach(this::revoke);
    }

    /**
     * Revokes every active refresh token belonging to a user.
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        tokens.forEach(this::revoke);
        log.info("Revoked {} refresh token(s) for userId={}", tokens.size(), userId);
    }

    // -- Private helpers -----------------------------------------------------

    private void persist(Long userId, String sessionId, String raw, String replacedBy) {
        Claims claims = jwtProvider.validateToken(raw).orElseThrow();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());

        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .sessionId(sessionId)
                .tokenHash(Hashing.sha256Hex(raw))
                .jti(jwtProvider.extractTokenId(claims))
                .expiresAt(expiresAt)
                .revoked(false)
                .replacedBy(replacedBy)
                .createdAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(token);
    }

    private void revoke(RefreshToken token) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }
}
