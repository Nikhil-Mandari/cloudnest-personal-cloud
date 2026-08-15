package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.LoginHistoryEntry;
import com.cloudnest.auth.dto.PageResponse;
import com.cloudnest.auth.dto.SecurityLogEntry;
import com.cloudnest.auth.dto.SecurityOverview;
import com.cloudnest.auth.dto.SessionInfo;
import com.cloudnest.auth.dto.TrustedDeviceInfo;
import com.cloudnest.auth.entity.LoginHistory;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.SecurityLog;
import com.cloudnest.auth.entity.TrustedDevice;
import com.cloudnest.auth.repository.LoginHistoryRepository;
import com.cloudnest.auth.repository.PasskeyCredentialRepository;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import com.cloudnest.auth.repository.SecurityLogRepository;
import com.cloudnest.auth.repository.TrustedDeviceRepository;
import com.cloudnest.auth.service.SecurityService;
import com.cloudnest.auth.service.TwoFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Security page backing service: sessions (from refresh tokens), trusted
 * devices, login history, security log and the aggregated overview.
 */
@Slf4j
@Service
public class SecurityServiceImpl implements SecurityService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityLogRepository securityLogRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final TwoFactorService twoFactorService;

    public SecurityServiceImpl(RefreshTokenRepository refreshTokenRepository,
                               TrustedDeviceRepository trustedDeviceRepository,
                               LoginHistoryRepository loginHistoryRepository,
                               SecurityLogRepository securityLogRepository,
                               PasskeyCredentialRepository passkeyCredentialRepository,
                               TwoFactorService twoFactorService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.securityLogRepository = securityLogRepository;
        this.passkeyCredentialRepository = passkeyCredentialRepository;
        this.twoFactorService = twoFactorService;
    }

    // ── Sessions ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<SessionInfo> getSessions(Long userId, String currentDeviceId) {
        return refreshTokenRepository.findByUserIdAndRevokedFalse(userId).stream()
                .map(token -> toSessionInfo(token, currentDeviceId, userId))
                .toList();
    }

    private SessionInfo toSessionInfo(RefreshToken token, String currentDeviceId, Long userId) {
        String deviceId = token.getDeviceId();
        boolean trusted = deviceId != null
                && trustedDeviceRepository.existsByUserIdAndDeviceId(userId, deviceId);
        return SessionInfo.builder()
                .sessionId(String.valueOf(token.getId()))
                .deviceId(deviceId != null ? deviceId : "")
                .deviceName(token.getDeviceName() != null ? token.getDeviceName() : "Browser session")
                .browser(token.getBrowser() != null ? token.getBrowser() : "Unknown")
                .os(token.getOs() != null ? token.getOs() : "Unknown")
                .deviceType(token.getDeviceType() != null ? token.getDeviceType() : "OTHER")
                .ipAddress(token.getIpAddress())
                .location(token.getLocation() != null ? token.getLocation() : "Unknown")
                .current(currentDeviceId != null && currentDeviceId.equals(deviceId))
                .trusted(trusted)
                .loginTime(token.getCreatedAt())
                .lastActive(token.getLastActiveAt() != null ? token.getLastActiveAt() : token.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void endSession(Long userId, String sessionId) {
        Long id = parseSessionId(sessionId);
        refreshTokenRepository.findById(id)
                .filter(token -> token.getUserId().equals(userId))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    logEvent(userId, "SESSION_ENDED", "Session ended remotely");
                    log.info("Session {} ended for userId={}", sessionId, userId);
                });
    }

    private Long parseSessionId(String sessionId) {
        try {
            return Long.parseLong(sessionId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid session id");
        }
    }

    // ── Trusted devices ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TrustedDeviceInfo> getTrustedDevices(Long userId) {
        return trustedDeviceRepository.findByUserId(userId).stream()
                .map(d -> TrustedDeviceInfo.builder()
                        .id(d.getId())
                        .deviceId(d.getDeviceId())
                        .deviceName(d.getDeviceName() != null ? d.getDeviceName() : "Browser session")
                        .browser(d.getBrowser() != null ? d.getBrowser() : "Unknown")
                        .os(d.getOs() != null ? d.getOs() : "Unknown")
                        .ipAddress(d.getIpAddress())
                        .lastUsedAt(d.getLastUsedAt())
                        .createdAt(d.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void removeTrustedDevice(Long userId, Long id) {
        trustedDeviceRepository.deleteByIdAndUserId(id, userId);
        logEvent(userId, "DEVICE_UNTRUSTED", "Trusted device removed");
    }

    @Override
    @Transactional
    public void trustDevice(Long userId, DeviceInfo device) {
        if (device == null || device.getDeviceId() == null || device.getDeviceId().isBlank()) {
            return;
        }
        TrustedDevice existing = trustedDeviceRepository
                .findByUserIdAndDeviceId(userId, device.getDeviceId())
                .orElseGet(() -> TrustedDevice.builder()
                        .userId(userId)
                        .deviceId(device.getDeviceId())
                        .build());
        existing.setDeviceName(device.getDeviceName());
        existing.setBrowser(device.getBrowser());
        existing.setOs(device.getOs());
        existing.setIpAddress(device.getIpAddress());
        existing.setLastUsedAt(LocalDateTime.now());
        trustedDeviceRepository.save(existing);
        logEvent(userId, "DEVICE_TRUSTED", "Device marked trusted: " + device.getDeviceId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeviceTrusted(Long userId, String deviceId) {
        return deviceId != null
                && trustedDeviceRepository.existsByUserIdAndDeviceId(userId, deviceId);
    }

    // ── Login history / security log ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryEntry> getLoginHistory(Long userId, int page, int size) {
        Page<LoginHistory> result = loginHistoryRepository.findByUserId(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loginTime")));
        return PageResponse.from(result, entry -> LoginHistoryEntry.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .success(entry.getSuccess())
                .ipAddress(entry.getIpAddress())
                .browser(entry.getBrowser())
                .os(entry.getOs())
                .deviceType(entry.getDeviceType())
                .deviceName(entry.getDeviceName())
                .location(entry.getLocation())
                .failureReason(entry.getFailureReason())
                .loginTime(entry.getLoginTime())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SecurityLogEntry> getSecurityLogs(Long userId, int page, int size) {
        Page<SecurityLog> result = securityLogRepository.findByUserId(
                userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, entry -> SecurityLogEntry.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .action(entry.getAction())
                .details(entry.getDetails())
                .ipAddress(entry.getIpAddress())
                .browser(entry.getBrowser())
                .os(entry.getOs())
                .location(entry.getLocation())
                .createdAt(entry.getCreatedAt())
                .build());
    }

    @Override
    @Transactional
    public void recordLoginSuccess(Long userId, DeviceInfo device) {
        loginHistoryRepository.save(LoginHistory.builder()
                .userId(userId)
                .success(true)
                .ipAddress(device != null ? device.getIpAddress() : null)
                .browser(device != null ? device.getBrowser() : null)
                .os(device != null ? device.getOs() : null)
                .deviceType(device != null ? device.getDeviceType() : null)
                .deviceName(device != null ? device.getDeviceName() : null)
                .location(device != null ? device.getLocation() : null)
                .loginTime(LocalDateTime.now())
                .build());
        logEvent(userId, "LOGIN_SUCCESS", "Signed in");
    }

    @Override
    @Transactional
    public void recordLoginFailure(Long userId, String failureReason, DeviceInfo device) {
        loginHistoryRepository.save(LoginHistory.builder()
                .userId(userId)
                .success(false)
                .ipAddress(device != null ? device.getIpAddress() : null)
                .browser(device != null ? device.getBrowser() : null)
                .os(device != null ? device.getOs() : null)
                .deviceType(device != null ? device.getDeviceType() : null)
                .deviceName(device != null ? device.getDeviceName() : null)
                .location(device != null ? device.getLocation() : null)
                .failureReason(failureReason)
                .loginTime(LocalDateTime.now())
                .build());
        if (userId != null) {
            logEvent(userId, "LOGIN_FAILED", "Failed sign-in" + (failureReason != null ? ": " + failureReason : ""));
        }
    }

    @Override
    @Transactional
    public void logEvent(Long userId, String action, String details) {
        if (userId == null) {
            return;
        }
        securityLogRepository.save(SecurityLog.builder()
                .userId(userId)
                .action(action)
                .details(details)
                .build());
    }

    // ── Overview ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public SecurityOverview getOverview(Long userId, boolean emailVerified) {
        long activeSessions = refreshTokenRepository.findByUserIdAndRevokedFalse(userId).size();
        long trustedDevices = trustedDeviceRepository.countByUserId(userId);
        long failed7d = loginHistoryRepository
                .countByUserIdAndSuccessFalseAndLoginTimeAfter(userId, LocalDateTime.now().minusDays(7));
        long totalLogins = loginHistoryRepository.countByUserIdAndSuccessTrue(userId);
        LoginHistory lastLogin = loginHistoryRepository
                .findFirstByUserIdAndSuccessTrueOrderByLoginTimeDesc(userId);
        boolean twoFactor = twoFactorService.isEnabled(userId);
        long passkeyCount = passkeyCredentialRepository.countByUserId(userId);

        int score = computeScore(emailVerified, twoFactor, passkeyCount, failed7d, totalLogins > 0);

        return SecurityOverview.builder()
                .securityScore(score)
                .accountStatus(emailVerified ? "ACTIVE" : "PENDING_VERIFICATION")
                .emailVerified(emailVerified)
                .twoFactorEnabled(twoFactor)
                .lastLoginAt(lastLogin != null ? lastLogin.getLoginTime() : null)
                .activeSessionCount(activeSessions)
                .trustedDeviceCount(trustedDevices)
                .failedLoginsLast7Days(failed7d)
                .totalLogins(totalLogins)
                .build();
    }

    private int computeScore(boolean emailVerified, boolean twoFactor, long passkeyCount,
                             long failed7d, boolean hasLogins) {
        int score = 0;
        if (emailVerified) score += 20;
        if (twoFactor) score += 25;
        if (passkeyCount > 0) score += 15;
        if (hasLogins) score += 10;
        if (failed7d == 0) {
            score += 20;
        } else {
            score += Math.max(0, 20 - (int) failed7d * 5);
        }
        return Math.max(0, Math.min(100, score));
    }
}
