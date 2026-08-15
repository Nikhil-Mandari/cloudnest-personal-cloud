package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.LoginHistoryEntry;
import com.cloudnest.auth.dto.PageResponse;
import com.cloudnest.auth.dto.SecurityLogEntry;
import com.cloudnest.auth.dto.SecurityOverview;
import com.cloudnest.auth.dto.SessionInfo;
import com.cloudnest.auth.dto.TrustedDeviceInfo;

import java.util.List;

/**
 * Session / device / audit operations backing the Security page.
 */
public interface SecurityService {

    // ── Sessions (derived from active refresh tokens) ─────────────────────

    List<SessionInfo> getSessions(Long userId, String currentDeviceId);

    /** Revokes a session (its refresh token) unless it belongs to another user. */
    void endSession(Long userId, String sessionId);

    // ── Trusted devices ────────────────────────────────────────────────────

    List<TrustedDeviceInfo> getTrustedDevices(Long userId);

    void removeTrustedDevice(Long userId, Long id);

    /** Marks the device as trusted (idempotent). */
    void trustDevice(Long userId, DeviceInfo device);

    boolean isDeviceTrusted(Long userId, String deviceId);

    // ── Login history / security log ───────────────────────────────────────

    PageResponse<LoginHistoryEntry> getLoginHistory(Long userId, int page, int size);

    PageResponse<SecurityLogEntry> getSecurityLogs(Long userId, int page, int size);

    /** Records a successful login + security event. */
    void recordLoginSuccess(Long userId, DeviceInfo device);

    /** Records a failed login attempt. */
    void recordLoginFailure(Long userId, String failureReason, DeviceInfo device);

    /** Records an arbitrary security event (2FA, passkey, password, ...). */
    void logEvent(Long userId, String action, String details);

    // ── Overview ───────────────────────────────────────────────────────────

    SecurityOverview getOverview(Long userId, boolean emailVerified);
}
