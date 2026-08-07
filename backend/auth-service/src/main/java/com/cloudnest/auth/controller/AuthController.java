package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.AdminSecurityOverviewResponse;
import com.cloudnest.auth.dto.AdminUserResponse;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.ChangePasswordRequest;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginHistoryResponse;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.LogoutRequest;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RefreshTokenRequest;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;
import com.cloudnest.auth.dto.ResendOtpRequest;
import com.cloudnest.auth.dto.SecurityLogResponse;
import com.cloudnest.auth.dto.SecurityOverviewResponse;
import com.cloudnest.auth.dto.SessionResponse;
import com.cloudnest.auth.dto.TrustedDeviceResponse;
import com.cloudnest.auth.dto.VerifyOtpRequest;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.security.DeviceInfo;
import com.cloudnest.auth.security.DeviceInfoParser;
import com.cloudnest.auth.security.IpUtils;
import com.cloudnest.auth.security.LocationResolver;
import com.cloudnest.auth.service.AuthService;
import com.cloudnest.auth.util.StandardResponse;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.cloudnest.auth.util.AdminGuard;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for the enterprise authentication API.
 * <p>
 * Public endpoints: register (+ verify), login (+ verify), OTP resend,
 * forgot-password (+ verify/reset), refresh. Protected endpoints: validate,
 * change-password, logout(-all), sessions, trusted devices, login history,
 * security logs and the security overview.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, OTP login, password reset, sessions, devices and security.")
public class AuthController {

    /** Header carrying the stable client device id (set by the frontend). */
    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final LocationResolver locationResolver;

    public AuthController(AuthService authService,
                          JwtProvider jwtProvider,
                          LocationResolver locationResolver) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
        this.locationResolver = locationResolver;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Registers a user in the pending state and emails an activation OTP.
     */
    @Operation(summary = "Register a new account",
            description = "Creates the account in PENDING_VERIFICATION state and emails a 6-digit OTP. "
                    + "The account is only activated after POST /api/auth/register/verify.")
    @PostMapping("/register")
    public ResponseEntity<StandardResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/register - username={}", request.getUsername());

        RegisterResponse response = authService.register(request, clientInfo(httpRequest));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<RegisterResponse>builder()
                        .success(true)
                        .message("Registration initiated — verify your email to activate the account")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Verifies the registration OTP, activates the account and signs the user
     * in.
     */
    @Operation(summary = "Verify registration OTP",
            description = "Activates the account and returns a JWT token pair (auto sign-in).")
    @PostMapping("/register/verify")
    public ResponseEntity<StandardResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId,
            @RequestParam(defaultValue = "false") boolean rememberDevice,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/register/verify - email={}", maskEmail(request.getEmail()));

        AuthResponse response = authService.verifyRegistration(
                request, clientInfo(httpRequest), deviceId, rememberDevice);

        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Account activated — welcome to CloudNest!")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Login
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Authenticates the password and either returns a token pair (trusted
     * device) or an OTP challenge.
     */
    @Operation(summary = "Sign in",
            description = "Validates the password, then either returns a JWT token pair "
                    + "(trusted device with OTP skipping) or requires an emailed OTP via "
                    + "POST /api/auth/login/verify.")
    @PostMapping("/login")
    public ResponseEntity<StandardResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login - usernameOrEmail={}", maskEmail(request.getUsernameOrEmail()));

        LoginResponse response = authService.login(request, clientInfo(httpRequest), deviceId);

        return ResponseEntity.ok(StandardResponse.<LoginResponse>builder()
                .success(true)
                .message(response.isRequiresOtp()
                        ? "Password verified — enter the code sent to your email"
                        : "Login successful")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Verifies the login OTP and completes the sign-in.
     */
    @Operation(summary = "Verify login OTP",
            description = "Completes a sign-in after the user enters the emailed code.")
    @PostMapping("/login/verify")
    public ResponseEntity<StandardResponse<AuthResponse>> verifyLogin(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId,
            @RequestParam(defaultValue = "false") boolean rememberDevice,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login/verify");

        AuthResponse response = authService.verifyLogin(
                request, clientInfo(httpRequest), deviceId, rememberDevice);

        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successful")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Resends an OTP (cooldown-bounded) for registration, login or reset.
     */
    @Operation(summary = "Resend OTP",
            description = "Requests a fresh OTP for registration, login or password reset. "
                    + "Rate-limited by a resend cooldown.")
    @PostMapping("/otp/resend")
    public ResponseEntity<StandardResponse<OtpDispatchResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/otp/resend");

        OtpDispatchResponse response = authService.resendOtp(request, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<OtpDispatchResponse>builder()
                .success(true)
                .message("A new code has been sent")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Forgot password
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Starts the password-reset flow by emailing an OTP.
     */
    @Operation(summary = "Request password reset",
            description = "Emails a reset OTP. Always returns the same generic response "
                    + "whether or not the account exists.")
    @PostMapping("/forgot-password")
    public ResponseEntity<StandardResponse<OtpDispatchResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password");

        OtpDispatchResponse response = authService.forgotPassword(request, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<OtpDispatchResponse>builder()
                .success(true)
                .message("If an account exists for that email, a reset code has been sent")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Verifies the reset OTP and returns a short-lived reset token.
     */
    @Operation(summary = "Verify password-reset OTP",
            description = "Confirms the emailed code and returns a short-lived reset token.")
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<StandardResponse<ResetTokenResponse>> verifyForgotPasswordOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password/verify");

        ResetTokenResponse response = authService.verifyForgotPasswordOtp(request, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<ResetTokenResponse>builder()
                .success(true)
                .message("Code verified — set your new password")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Sets the new password, ending all sessions and revoking refresh tokens.
     */
    @Operation(summary = "Reset password",
            description = "Sets the new password, revokes every refresh token and ends all sessions.")
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<StandardResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password/reset");

        authService.resetPassword(request, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Password reset successfully — please sign in")
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tokens
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Rotates the refresh token and returns a fresh token pair.
     */
    @Operation(summary = "Refresh tokens",
            description = "Rotates the refresh token (old one is revoked) and returns a new access + refresh pair.")
    @PostMapping("/refresh")
    public ResponseEntity<StandardResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.debug("POST /api/auth/refresh");

        AuthResponse response = authService.refresh(request.getRefreshToken(), clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Tokens refreshed")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Validates an access token (existing contract, unchanged).
     */
    @Operation(summary = "Validate JWT",
            description = "Validates an access token and returns the associated user details.")
    @GetMapping("/validate")
    public ResponseEntity<StandardResponse<AuthResponse>> validate(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("GET /api/auth/validate - missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StandardResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Missing or invalid Authorization header")
                            .path(httpRequest.getRequestURI())
                            .build());
        }

        String token = authHeader.substring(7);
        AuthResponse authResponse = authService.validateToken(token);

        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Token is valid")
                .data(authResponse)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Ends the current session and revokes its refresh tokens.
     */
    @Operation(summary = "Sign out",
            description = "Ends the caller's session and revokes its refresh tokens.")
    @PostMapping("/logout")
    public ResponseEntity<StandardResponse<Void>> logout(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/logout - userId={}", userIdHeader);

        String sessionId = extractSessionId(httpRequest);
        authService.logout(userIdHeader, sessionId, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Logged out successfully")
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Ends every session and revokes all refresh tokens.
     */
    @Operation(summary = "Sign out everywhere",
            description = "Ends every active session and revokes all refresh tokens.")
    @PostMapping("/logout-all")
    public ResponseEntity<StandardResponse<Void>> logoutAll(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/logout-all - userId={}", userIdHeader);

        authService.logoutAll(userIdHeader, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Logged out from all devices")
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Password change
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Changes the password (existing contract, enhanced with revocation).
     */
    @Operation(summary = "Change password",
            description = "Verifies the current password, applies the new one and revokes all refresh tokens.")
    @PutMapping("/change-password")
    public ResponseEntity<StandardResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/auth/change-password - userId={}", userIdHeader);

        authService.changePassword(userIdHeader, request, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Password changed successfully")
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sessions / devices / history / overview
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Lists active sessions, marking the caller's as current.
     */
    @Operation(summary = "Active sessions",
            description = "Lists the user's active sessions (devices).")
    @GetMapping("/sessions")
    public ResponseEntity<StandardResponse<List<SessionResponse>>> getSessions(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        List<SessionResponse> sessions = authService.getSessions(userIdHeader, extractSessionId(httpRequest));

        return ResponseEntity.ok(StandardResponse.<List<SessionResponse>>builder()
                .success(true)
                .message("Sessions retrieved successfully")
                .data(sessions)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Ends a specific session remotely.
     */
    @Operation(summary = "End a session",
            description = "Ends the given session and revokes its refresh tokens.")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<StandardResponse<Void>> endSession(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @PathVariable String sessionId,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/auth/sessions/{} - userId={}", sessionId, userIdHeader);

        authService.endSession(userIdHeader, sessionId, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Session ended")
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Lists the user's trusted devices.
     */
    @Operation(summary = "Trusted devices",
            description = "Lists devices the user has marked as trusted.")
    @GetMapping("/trusted-devices")
    public ResponseEntity<StandardResponse<List<TrustedDeviceResponse>>> getTrustedDevices(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        List<TrustedDeviceResponse> devices = authService.getTrustedDevices(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<List<TrustedDeviceResponse>>builder()
                .success(true)
                .message("Trusted devices retrieved successfully")
                .data(devices)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Removes a trusted device.
     */
    @Operation(summary = "Remove a trusted device",
            description = "Stops trusting a device; future logins from it will require OTP again.")
    @DeleteMapping("/trusted-devices/{id}")
    public ResponseEntity<StandardResponse<Void>> removeTrustedDevice(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/auth/trusted-devices/{} - userId={}", id, userIdHeader);

        authService.removeTrustedDevice(userIdHeader, id, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Trusted device removed")
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Paginated sign-in history.
     */
    @Operation(summary = "Login history",
            description = "Paginated record of successful and failed sign-in attempts.")
    @GetMapping("/login-history")
    public ResponseEntity<StandardResponse<Page<LoginHistoryResponse>>> getLoginHistory(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<LoginHistoryResponse> history = authService.getLoginHistory(userIdHeader, page, Math.min(size, 100));

        return ResponseEntity.ok(StandardResponse.<Page<LoginHistoryResponse>>builder()
                .success(true)
                .message("Login history retrieved successfully")
                .data(history)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Paginated security log.
     */
    @Operation(summary = "Security log",
            description = "Paginated audit trail of security-relevant actions.")
    @GetMapping("/security-logs")
    public ResponseEntity<StandardResponse<Page<SecurityLogResponse>>> getSecurityLogs(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<SecurityLogResponse> logs = authService.getSecurityLogs(userIdHeader, page, Math.min(size, 100));

        return ResponseEntity.ok(StandardResponse.<Page<SecurityLogResponse>>builder()
                .success(true)
                .message("Security logs retrieved successfully")
                .data(logs)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Aggregated security posture.
     */
    @Operation(summary = "Security overview",
            description = "Security score, account state, session/device counts and recent activity signals.")
    @GetMapping("/security-overview")
    public ResponseEntity<StandardResponse<SecurityOverviewResponse>> getSecurityOverview(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        SecurityOverviewResponse overview = authService.getSecurityOverview(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<SecurityOverviewResponse>builder()
                .success(true)
                .message("Security overview retrieved successfully")
                .data(overview)
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Admin (platform-wide, ROLE_ADMIN only)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Platform-wide security overview (admin).
     */
    @Operation(summary = "Admin security overview",
            description = "Platform-wide account mix, login volume and session/device totals. Requires ROLE_ADMIN.")
    @GetMapping("/admin/security-overview")
    public ResponseEntity<StandardResponse<AdminSecurityOverviewResponse>> getAdminSecurityOverview(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/auth/admin/security-overview");

        AdminSecurityOverviewResponse overview = authService.getAdminSecurityOverview();

        return ResponseEntity.ok(StandardResponse.<AdminSecurityOverviewResponse>builder()
                .success(true)
                .message("Admin security overview retrieved successfully")
                .data(overview)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Sign-in history across every user (admin).
     */
    @Operation(summary = "Admin login history",
            description = "Paginated sign-in history across every user. Requires ROLE_ADMIN.")
    @GetMapping("/admin/login-history")
    public ResponseEntity<StandardResponse<Page<LoginHistoryResponse>>> getAdminLoginHistory(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/auth/admin/login-history?page={}&size={}", page, size);

        Page<LoginHistoryResponse> history = authService.getAdminLoginHistory(page, Math.min(size, 100));

        return ResponseEntity.ok(StandardResponse.<Page<LoginHistoryResponse>>builder()
                .success(true)
                .message("Admin login history retrieved successfully")
                .data(history)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Security log across every user (admin).
     */
    @Operation(summary = "Admin security log",
            description = "Paginated security log across every user. Requires ROLE_ADMIN.")
    @GetMapping("/admin/security-logs")
    public ResponseEntity<StandardResponse<Page<SecurityLogResponse>>> getAdminSecurityLogs(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/auth/admin/security-logs?page={}&size={}", page, size);

        Page<SecurityLogResponse> logs = authService.getAdminSecurityLogs(page, Math.min(size, 100));

        return ResponseEntity.ok(StandardResponse.<Page<SecurityLogResponse>>builder()
                .success(true)
                .message("Admin security logs retrieved successfully")
                .data(logs)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Enables or disables a user account (admin). Disabling ends every
     * session and revokes all refresh tokens.
     */
    @Operation(summary = "Enable / disable a user",
            description = "Toggles a user's enabled flag. Disabling signs the user out everywhere. Requires ROLE_ADMIN.")
    @PatchMapping("/admin/users/{id}/enabled")
    public ResponseEntity<StandardResponse<AdminUserResponse>> setUserEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader("X-User-Id") Long adminId,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("PATCH /api/auth/admin/users/{}/enabled?enabled={} by admin {}", id, enabled, adminId);

        AdminUserResponse response = authService.setUserEnabled(id, enabled, adminId, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<AdminUserResponse>builder()
                .success(true)
                .message(enabled ? "User enabled" : "User disabled")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Changes a user's role (admin).
     */
    @Operation(summary = "Change a user's role",
            description = "Sets ROLE_ADMIN or ROLE_USER on a user. Requires ROLE_ADMIN.")
    @PatchMapping("/admin/users/{id}/role")
    public ResponseEntity<StandardResponse<AdminUserResponse>> setUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader("X-User-Id") Long adminId,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("PATCH /api/auth/admin/users/{}/role?role={} by admin {}", id, role, adminId);

        AdminUserResponse response = authService.setUserRole(id, role, adminId, clientInfo(httpRequest));

        return ResponseEntity.ok(StandardResponse.<AdminUserResponse>builder()
                .success(true)
                .message("Role updated")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════

    private ClientInfo clientInfo(HttpServletRequest request) {
        DeviceInfo device = DeviceInfoParser.parse(
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(DEVICE_ID_HEADER));
        String ip = IpUtils.resolve(request);
        return new ClientInfo(ip, device, locationResolver.resolve(ip));
    }

    /**
     * Extracts the session id (sid claim) from the bearer access token.
     */
    private String extractSessionId(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return jwtProvider.validateToken(authHeader.substring(7))
                .map(jwtProvider::extractSessionId)
                .orElse(null);
    }

    /**
     * Masks an email for logging: j***@example.com.
     */
    private String maskEmail(String value) {
        if (value == null) {
            return null;
        }
        int at = value.indexOf('@');
        if (at <= 1) {
            return "***";
        }
        return value.charAt(0) + "***" + value.substring(at);
    }
}
