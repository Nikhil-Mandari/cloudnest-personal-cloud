package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.mapper.UserMapper;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Issues the standard JWT + opaque refresh-token pair used by every sign-in
 * path (password, OTP, OAuth, passkey, 2FA). Refresh tokens carry the device
 * metadata used by the Security page sessions list.
 */
@Slf4j
@Service
public class TokenIssuer {

    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenIssuer(JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /** Generates the JWT and persists a new refresh token (with device metadata). */
    @Transactional
    public String issueRefreshToken(Long userId, DeviceInfo device) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                .deviceId(device != null ? device.getDeviceId() : null)
                .deviceName(device != null ? device.getDeviceName() : null)
                .browser(device != null ? device.getBrowser() : null)
                .os(device != null ? device.getOs() : null)
                .deviceType(device != null ? device.getDeviceType() : null)
                .ipAddress(device != null ? device.getIpAddress() : null)
                .location(device != null ? device.getLocation() : null)
                .lastActiveAt(LocalDateTime.now())
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /** Issues a complete JWT + refresh token pair. */
    @Transactional
    public AuthResponse issue(UserCredential user, DeviceInfo device) {
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        String refreshToken = issueRefreshToken(user.getId(), device);
        return UserMapper.toAuthResponse(user, token, refreshToken);
    }

    /** SHA-256 hash of a raw refresh token (never store plain text). */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
