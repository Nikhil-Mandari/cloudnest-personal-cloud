package com.cloudnest.auth.controller;

import com.cloudnest.auth.client.UserServiceClient;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.ChangePasswordRequest;
import com.cloudnest.auth.dto.CreateProfileRequest;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RefreshTokenRequest;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResendOtpRequest;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;
import com.cloudnest.auth.dto.TwoFactorLoginRequest;
import com.cloudnest.auth.dto.VerifyOtpRequest;
import com.cloudnest.auth.service.AuthService;
import com.cloudnest.auth.util.DeviceInfoParser;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 * <p>
 * Provides endpoints for user registration, login (with optional OTP and
 * TOTP 2FA steps), JWT token validation, refresh-token rotation and the
 * password flows.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserServiceClient userServiceClient;
    private final DeviceInfoParser deviceInfoParser;

    public AuthController(AuthService authService,
                          UserServiceClient userServiceClient,
                          DeviceInfoParser deviceInfoParser) {
        this.authService = authService;
        this.userServiceClient = userServiceClient;
        this.deviceInfoParser = deviceInfoParser;
    }

    /**
     * Registers a new user account (OTP verification pending).
     *
     * @param request the registration payload (username, email, password)
     * @return 201 Created with the OTP dispatch details (no JWT yet)
     */
    @PostMapping("/register")
    public ResponseEntity<StandardResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/register - username={}", request.getUsername());

        RegisterResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<RegisterResponse>builder()
                        .success(true)
                        .message("User registered successfully. Verify your email to activate the account.")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Verifies the registration OTP and activates the account.
     *
     * @param request        the OTP verification payload (email + code)
     * @param rememberDevice when true the device is trusted (skips future OTP)
     * @return 200 OK with the JWT token and user details
     */
    @PostMapping("/register/verify")
    public ResponseEntity<StandardResponse<AuthResponse>> verifyRegistration(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestParam(defaultValue = "false") boolean rememberDevice,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/register/verify - email={}", request.getEmail());

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        AuthResponse authResponse = authService.verifyRegistration(request.getEmail(), request.getCode(), rememberDevice, device);

        // Provision the user profile in user-service now that the account is active
        provisionProfile(authResponse);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Email verified successfully. Account activated.")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Resends the registration OTP.
     *
     * @param request the resend payload (email)
     * @return 200 OK with the new OTP dispatch details
     */
    @PostMapping("/otp/resend")
    public ResponseEntity<StandardResponse<OtpDispatchResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/otp/resend - email={}", request.getEmail());

        OtpDispatchResponse response = authService.resendOtp(request.getEmail(), request.getChallengeToken());

        return ResponseEntity.ok(
                StandardResponse.<OtpDispatchResponse>builder()
                        .success(true)
                        .message("OTP resent successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Best-effort profile provisioning: creates the matching profile in the
     * User Service ({@code user_db.users}) using the same numeric user ID.
     * <p>
     * Registration must NOT fail when the User Service is unavailable, so any
     * failure is logged and swallowed — the missing profile is healed lazily
     * by {@code GET /api/users/me} the next time it is requested.
     *
     * @param authResponse the successful registration result
     */
    private void provisionProfile(AuthResponse authResponse) {
        try {
            CreateProfileRequest profile = CreateProfileRequest.builder()
                    .id(authResponse.getUserId())
                    .username(authResponse.getUsername())
                    .email(authResponse.getEmail())
                    .role(authResponse.getRole())
                    .build();
            userServiceClient.createProfile(profile);
            log.info("User profile provisioned via user-service: userId={}", authResponse.getUserId());
        } catch (Exception e) {
            log.warn("Profile provisioning skipped/failed for userId={} — registration continues: {}",
                    authResponse.getUserId(), e.getMessage());
        }
    }

    /**
     * Authenticates a user and returns a JWT token, an OTP challenge (pending
     * accounts) or a TOTP 2FA challenge.
     *
     * @param request the login payload (username/email and password)
     * @return 200 OK with the JWT token and user details, or a challenge
     */
    @PostMapping("/login")
    public ResponseEntity<StandardResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login - usernameOrEmail={}", request.getUsernameOrEmail());

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        LoginResponse response = authService.login(request, device);

        String message;
        if (response.isRequires2fa()) {
            message = "Password verified. Enter your authenticator code.";
        } else if (response.isRequiresOtp()) {
            message = "Password verified. An OTP has been sent to your email.";
        } else {
            message = "Login successful";
        }

        return ResponseEntity.ok(
                StandardResponse.<LoginResponse>builder()
                        .success(true)
                        .message(message)
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Verifies the login OTP and returns a JWT token.
     *
     * @param request        the OTP verification payload (challengeToken + code)
     * @param rememberDevice when true the device is trusted (skips future OTP)
     * @return 200 OK with the JWT token and user details
     */
    @PostMapping("/login/verify")
    public ResponseEntity<StandardResponse<AuthResponse>> verifyLogin(
            @Valid @RequestBody VerifyOtpRequest request,
            @RequestParam(defaultValue = "false") boolean rememberDevice,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login/verify - challengeToken={}", request.getChallengeToken());

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        AuthResponse authResponse = authService.verifyLogin(request.getChallengeToken(), request.getCode(), rememberDevice, device);

        // Best-effort profile provisioning (idempotent in user-service) — the
        // profile may be missing if registration provisioning failed earlier.
        provisionProfile(authResponse);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Login verified successfully")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Completes a login blocked on the 2FA step (TOTP or backup code).
     *
     * @param request        the 2FA payload (challengeToken + code)
     * @param rememberDevice when true the device is trusted (skips 2FA later)
     * @return 200 OK with the JWT token and user details
     */
    @PostMapping("/login/2fa")
    public ResponseEntity<StandardResponse<AuthResponse>> verifyTwoFactorLogin(
            @Valid @RequestBody TwoFactorLoginRequest request,
            @RequestParam(defaultValue = "false") boolean rememberDevice,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login/2fa - challengeToken={}", request.getChallengeToken());

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        AuthResponse authResponse = authService.verifyTwoFactorLogin(
                request.getChallengeToken(), request.getCode(), rememberDevice, device);

        provisionProfile(authResponse);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Two-factor verification successful")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Rotates a refresh token into a fresh access-token pair.
     *
     * @param request the refresh token payload
     * @return 200 OK with a new JWT and rotated refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<StandardResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/refresh");

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken(), device);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Revokes the presented refresh token (logout).
     *
     * @param request optional refresh-token payload (may be absent)
     * @return 200 OK confirming the logout
     */
    @PostMapping("/logout")
    public ResponseEntity<StandardResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/logout");

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        authService.logout(request != null ? request.getRefreshToken() : null, device);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Logged out successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Revokes every refresh token belonging to the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by the gateway)
     * @return 200 OK confirming the logout
     */
    @PostMapping("/logout-all")
    public ResponseEntity<StandardResponse<Void>> logoutAll(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/logout-all - userId={}", userIdHeader);

        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        authService.logoutAll(userIdHeader, device);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Logged out of all devices successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Sends a password-reset OTP to the given email.
     *
     * @param request the forgot-password payload (email)
     * @return 200 OK with OTP dispatch details
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<StandardResponse<OtpDispatchResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password - email={}", request.getEmail());

        OtpDispatchResponse response = authService.forgotPassword(request);

        return ResponseEntity.ok(
                StandardResponse.<OtpDispatchResponse>builder()
                        .success(true)
                        .message("If an account exists for that email, a reset code has been sent.")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Verifies the password-reset OTP and returns a short-lived reset token.
     *
     * @param request the OTP verification payload (challengeToken + code)
     * @return 200 OK with the reset token
     */
    @PostMapping("/forgot-password/verify")
    public ResponseEntity<StandardResponse<ResetTokenResponse>> verifyForgotPassword(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password/verify - challengeToken={}", request.getChallengeToken());

        ResetTokenResponse response = authService.verifyForgotPassword(request.getChallengeToken(), request.getCode());

        return ResponseEntity.ok(
                StandardResponse.<ResetTokenResponse>builder()
                        .success(true)
                        .message("Reset code verified")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Sets a new password using a verified reset token.
     *
     * @param request the reset payload (resetToken + newPassword)
     * @return 200 OK confirming the password reset
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<StandardResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/forgot-password/reset");

        authService.resetPassword(request);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Changes the authenticated user's password after verifying the current one.
     *
     * @param userIdHeader the authenticated user's ID (set by the gateway)
     * @param request      the change-password payload
     * @return 200 OK confirming the password change
     */
    @PutMapping("/change-password")
    public ResponseEntity<StandardResponse<Void>> changePassword(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/auth/change-password - userId={}", userIdHeader);

        authService.changePassword(userIdHeader, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Password changed successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Validates a JWT token.
     * <p>
     * Expects the token in the {@code Authorization: Bearer <token>} header.
     *
     * @return 200 OK with the validated token and user details
     */
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
        log.debug("GET /api/auth/validate - validating token");

        AuthResponse authResponse = authService.validateToken(token);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Token is valid")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
