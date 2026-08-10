package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;
import com.cloudnest.auth.entity.OtpVerification;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.mapper.UserMapper;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.AuthService;
import com.cloudnest.auth.service.OtpService;
import com.cloudnest.auth.service.SecurityService;
import com.cloudnest.auth.service.TokenIssuer;
import com.cloudnest.auth.service.TwoFactorService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link AuthService} interface.
 * <p>
 * Handles user registration with OTP email verification, password-based
 * login (with optional TOTP / backup-code 2FA), JWT token validation,
 * refresh-token rotation, and password flows. Security-relevant events are
 * recorded to the login-history and security-log tables.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final OtpService otpService;
    private final TwoFactorService twoFactorService;
    private final SecurityService securityService;
    private final TokenIssuer tokenIssuer;

    /** Password-reset and 2FA-login challenge JWTs live for 15 minutes. */
    private static final long CHALLENGE_TOKEN_EXPIRY_MS = 15L * 60 * 1000;

    public AuthServiceImpl(UserCredentialRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           OtpService otpService,
                           TwoFactorService twoFactorService,
                           SecurityService securityService,
                           TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.otpService = otpService;
        this.twoFactorService = twoFactorService;
        this.securityService = securityService;
        this.tokenIssuer = tokenIssuer;
    }

    // ── Registration (OTP-verified) ─────────────────────────────────────────

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.debug("Registering new user: username={}, email={}", request.getUsername(), request.getEmail());

        // -- Check for duplicates ------------------------------------------------
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already taken", request.getUsername());
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' already registered", request.getEmail());
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        // -- Build and persist the user entity (disabled until OTP verified) -----
        UserCredential user = UserCredential.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .enabled(false)
                .build();

        user = userRepository.save(user);
        log.info("User registered (pending verification): id={}, username={}", user.getId(), user.getUsername());

        // -- Generate and dispatch OTP -------------------------------------------
        OtpDispatchResponse otpResponse = otpService.generateOtp(user.getEmail(), "REGISTRATION");

        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .message("Account created successfully. Please verify your email with the code sent.")
                .devOtp(otpResponse.getDevOtp())
                .resendAfterSeconds(otpResponse.getResendAfterSeconds())
                .otpExpiryMinutes(otpResponse.getOtpExpiryMinutes())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistration(String email, String code, boolean rememberDevice, DeviceInfo device) {
        log.debug("Verifying registration OTP for email={}", email);

        // Validate the OTP
        OtpVerification otp = otpService.verifyOtp(email, code, "REGISTRATION")
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code. Please request a new code."));

        // Find and enable the user
        UserCredential user = userRepository.findByEmail(otp.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + otp.getEmail()));

        user.setEnabled(true);
        userRepository.save(user);

        log.info("User enabled after OTP verification: id={}, username={}", user.getId(), user.getUsername());

        AuthResponse authResponse = tokenIssuer.issue(user, device);
        securityService.recordLoginSuccess(user.getId(), device);
        securityService.logEvent(user.getId(), "ACCOUNT_ACTIVATED", "Account activated after email verification");
        if (rememberDevice) {
            securityService.trustDevice(user.getId(), device);
        }
        return authResponse;
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendOtp(String email, String challengeToken) {
        log.debug("Resending OTP for email={}, challengeToken={}", email, challengeToken);

        // Verify the user exists
        userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + email));

        // Challenge-bound flows (login / password-reset) reuse the SAME
        // challenge token and preserve its stored purpose, so the frontend's
        // stored challengeToken remains valid for verification.
        if (challengeToken != null && !challengeToken.isBlank()) {
            String purpose = otpService.findPurposeByChallengeToken(challengeToken)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "This verification session has expired. Please start again."));
            return otpService.generateOtpWithChallenge(email, purpose, challengeToken);
        }

        // Registration flow: no challenge token
        return otpService.generateOtp(email, "REGISTRATION");
    }

    // ── Login (password + optional 2FA) ─────────────────────────────────────

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, DeviceInfo device) {
        log.debug("Login attempt: usernameOrEmail={}", request.getUsernameOrEmail());

        // -- Resolve user by username or email ------------------------------------
        UserCredential user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    log.warn("Login failed: user '{}' not found", request.getUsernameOrEmail());
                    return new UsernameNotFoundException("User not found: " + request.getUsernameOrEmail());
                });

        // -- Verify password ------------------------------------------------------
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user '{}'", user.getUsername());
            securityService.recordLoginFailure(user.getId(), "Invalid password", device);
            throw new BadCredentialsException("Invalid username/email or password");
        }

        // -- Check if OTP verification is required (user not yet enabled) ---------
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.info("Login requires OTP verification: userId={}, email={}", user.getId(), user.getEmail());
            String challengeToken = UUID.randomUUID().toString();

            OtpDispatchResponse otpResponse = otpService.generateOtpWithChallenge(
                    user.getEmail(), "LOGIN", challengeToken);

            return LoginResponse.builder()
                    .requiresOtp(true)
                    .challengeToken(challengeToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .devOtp(otpResponse.getDevOtp())
                    .resendAfterSeconds(otpResponse.getResendAfterSeconds())
                    .otpExpiryMinutes(otpResponse.getOtpExpiryMinutes())
                    .build();
        }

        // -- 2FA challenge when the device is not trusted -------------------------
        boolean trusted = securityService.isDeviceTrusted(user.getId(), device != null ? device.getDeviceId() : null);
        if (twoFactorService.isEnabled(user.getId()) && !trusted) {
            log.info("Login requires 2FA: userId={}, email={}", user.getId(), user.getEmail());
            String challengeToken = jwtProvider.generateTokenWithExpiry(
                    user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                    CHALLENGE_TOKEN_EXPIRY_MS, "LOGIN_2FA");

            return LoginResponse.builder()
                    .requiresOtp(false)
                    .requires2fa(true)
                    .challengeToken(challengeToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .trustedDevice(trusted)
                    .build();
        }

        log.info("User logged in successfully: id={}, username={}", user.getId(), user.getUsername());
        AuthResponse authResponse = tokenIssuer.issue(user, device);
        securityService.recordLoginSuccess(user.getId(), device);
        return LoginResponse.builder()
                .requiresOtp(false)
                .token(authResponse.getToken())
                .refreshToken(authResponse.getRefreshToken())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .trustedDevice(trusted)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyLogin(String challengeToken, String code, boolean rememberDevice, DeviceInfo device) {
        log.debug("Verifying login OTP for challengeToken={}", challengeToken);

        // Validate the OTP bound to the challenge
        OtpVerification otp = otpService.verifyOtpWithChallenge(challengeToken, code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification code. Please sign in again."));

        // Find the user
        UserCredential user = userRepository.findByEmail(otp.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + otp.getEmail()));

        // Enable the user if not yet enabled (registers after OTP login)
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            user.setEnabled(true);
            userRepository.save(user);
            log.info("User enabled after login OTP verification: id={}", user.getId());
        }

        AuthResponse authResponse = tokenIssuer.issue(user, device);
        securityService.recordLoginSuccess(user.getId(), device);
        if (rememberDevice) {
            securityService.trustDevice(user.getId(), device);
        }
        return authResponse;
    }

    @Override
    @Transactional
    public AuthResponse verifyTwoFactorLogin(String challengeToken, String code,
                                             boolean rememberDevice, DeviceInfo device) {
        log.debug("Verifying 2FA code for challengeToken={}", challengeToken);

        Claims claims = jwtProvider.validateToken(challengeToken)
                .orElseThrow(() -> new BadCredentialsException("This sign-in session has expired. Please sign in again."));

        if (!"LOGIN_2FA".equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Invalid challenge token");
        }

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new BadCredentialsException("Invalid challenge token");
        }

        if (!twoFactorService.verifyCode(userId, code)) {
            securityService.recordLoginFailure(userId, "Invalid 2FA code", device);
            throw new BadCredentialsException("That code did not work. Check your authenticator app and try again.");
        }

        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        AuthResponse authResponse = tokenIssuer.issue(user, device);
        securityService.recordLoginSuccess(user.getId(), device);
        securityService.logEvent(user.getId(), "2FA_VERIFIED", "Two-factor code verified");
        if (rememberDevice) {
            securityService.trustDevice(user.getId(), device);
        }
        return authResponse;
    }

    // ── Refresh-token rotation ───────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken, DeviceInfo device) {
        log.debug("Rotating refresh token");

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenIssuer.hashToken(refreshToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        UserCredential user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for refresh token"));

        // Rotate: revoke the presented token, issue a fresh pair
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        AuthResponse authResponse = tokenIssuer.issue(user, device);
        log.info("Refresh token rotated for userId={}", user.getId());
        return authResponse;
    }

    @Override
    @Transactional
    public void logout(String refreshToken, DeviceInfo device) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenIssuer.hashToken(refreshToken))
                .ifPresent(stored -> {
                    stored.setRevoked(true);
                    refreshTokenRepository.save(stored);
                    log.info("Refresh token revoked (logout) for userId={}", stored.getUserId());
                    securityService.logEvent(stored.getUserId(), "LOGOUT", "Signed out");
                });
    }

    @Override
    @Transactional
    public void logoutAll(Long userId, DeviceInfo device) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : active) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(active);
        securityService.logEvent(userId, "LOGOUT_ALL", "Signed out everywhere");
        log.info("All refresh tokens revoked for userId={}", userId);
    }

    // ── Password flows ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public OtpDispatchResponse forgotPassword(ForgotPasswordRequest request) {
        log.debug("Forgot-password request for email={}", request.getEmail());

        // Always return the same generic response whether or not the user exists,
        // to avoid leaking which emails have accounts.
        Optional<UserCredential> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return OtpDispatchResponse.builder()
                    .sent(false)
                    .resendAfterSeconds(30)
                    .otpExpiryMinutes(5)
                    .build();
        }

        String challengeToken = UUID.randomUUID().toString();
        return otpService.generateOtpWithChallenge(request.getEmail(), "PASSWORD_RESET", challengeToken);
    }

    @Override
    @Transactional
    public ResetTokenResponse verifyForgotPassword(String challengeToken, String code) {
        log.debug("Verifying forgot-password OTP");

        OtpVerification otp = otpService.verifyOtpWithChallenge(challengeToken, code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset code. Please request a new code."));

        UserCredential user = userRepository.findByEmail(otp.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email: " + otp.getEmail()));

        // Short-lived JWT that only grants permission to reset the password
        String resetToken = jwtProvider.generateTokenWithExpiry(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                CHALLENGE_TOKEN_EXPIRY_MS, "PASSWORD_RESET");

        log.info("Password-reset token issued for userId={}", user.getId());
        return ResetTokenResponse.builder().resetToken(resetToken).build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Claims claims = jwtProvider.validateToken(request.getResetToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));

        if (!"PASSWORD_RESET".equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Invalid reset token type");
        }

        Long userId = claims.get("userId", Long.class);
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for reset token"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all existing refresh tokens for the user
        logoutAll(userId, null);
        securityService.logEvent(userId, "PASSWORD_RESET", "Password reset");
        log.info("Password reset completed for userId={}", userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate existing refresh tokens so other sessions re-authenticate
        logoutAll(userId, null);
        securityService.logEvent(userId, "PASSWORD_CHANGED", "Password changed");
        log.info("Password changed for userId={}", userId);
    }

    // ── Token validation ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthResponse validateToken(String token) {
        log.debug("Validating JWT token");

        Claims claims = jwtProvider.validateToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new BadCredentialsException("Token does not contain a valid userId");
        }

        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Token validation failed: user not found for id={}", userId);
                    return new UsernameNotFoundException("User not found for id: " + userId);
                });

        log.debug("Token validated successfully for user: id={}, username={}", user.getId(), user.getUsername());

        return UserMapper.toAuthResponse(user, token);
    }

    /** Hourly sweep of expired refresh tokens. */
    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Cleanup sweep completed for expired refresh tokens");
    }
}
