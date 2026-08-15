package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.LoginHistoryEntry;
import com.cloudnest.auth.dto.PageResponse;
import com.cloudnest.auth.dto.SecurityLogEntry;
import com.cloudnest.auth.dto.SecurityOverview;
import com.cloudnest.auth.dto.SessionInfo;
import com.cloudnest.auth.dto.TrustedDeviceInfo;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.SecurityService;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Security page endpoints: active sessions, trusted devices, login history,
 * security log and the aggregated security overview.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class SecurityController {

    private final SecurityService securityService;
    private final UserCredentialRepository userRepository;

    public SecurityController(SecurityService securityService, UserCredentialRepository userRepository) {
        this.securityService = securityService;
        this.userRepository = userRepository;
    }

    @GetMapping("/sessions")
    public ResponseEntity<StandardResponse<List<SessionInfo>>> sessions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            HttpServletRequest httpRequest) {
        List<SessionInfo> sessions = securityService.getSessions(userId, deviceId);
        return ResponseEntity.ok(StandardResponse.<List<SessionInfo>>builder()
                .success(true)
                .message("Sessions retrieved")
                .data(sessions)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<StandardResponse<Void>> endSession(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("sessionId") String sessionId,
            HttpServletRequest httpRequest) {
        securityService.endSession(userId, sessionId);
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Session ended")
                .path(httpRequest.getRequestURI())
                .build());
    }

    @GetMapping("/trusted-devices")
    public ResponseEntity<StandardResponse<List<TrustedDeviceInfo>>> trustedDevices(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        List<TrustedDeviceInfo> devices = securityService.getTrustedDevices(userId);
        return ResponseEntity.ok(StandardResponse.<List<TrustedDeviceInfo>>builder()
                .success(true)
                .message("Trusted devices retrieved")
                .data(devices)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @DeleteMapping("/trusted-devices/{id}")
    public ResponseEntity<StandardResponse<Void>> removeTrustedDevice(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        securityService.removeTrustedDevice(userId, id);
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Trusted device removed")
                .path(httpRequest.getRequestURI())
                .build());
    }

    @GetMapping("/login-history")
    public ResponseEntity<StandardResponse<PageResponse<LoginHistoryEntry>>> loginHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        PageResponse<LoginHistoryEntry> response = securityService.getLoginHistory(userId, page, size);
        return ResponseEntity.ok(StandardResponse.<PageResponse<LoginHistoryEntry>>builder()
                .success(true)
                .message("Login history retrieved")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @GetMapping("/security-logs")
    public ResponseEntity<StandardResponse<PageResponse<SecurityLogEntry>>> securityLogs(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        PageResponse<SecurityLogEntry> response = securityService.getSecurityLogs(userId, page, size);
        return ResponseEntity.ok(StandardResponse.<PageResponse<SecurityLogEntry>>builder()
                .success(true)
                .message("Security log retrieved")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @GetMapping("/security-overview")
    public ResponseEntity<StandardResponse<SecurityOverview>> overview(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        boolean emailVerified = userRepository.findById(userId)
                .map(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElse(false);
        SecurityOverview overview = securityService.getOverview(userId, emailVerified);
        return ResponseEntity.ok(StandardResponse.<SecurityOverview>builder()
                .success(true)
                .message("Security overview retrieved")
                .data(overview)
                .path(httpRequest.getRequestURI())
                .build());
    }
}
