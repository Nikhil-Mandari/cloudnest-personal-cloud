package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.client.NotificationServiceClient;
import com.cloudnest.auth.client.UserServiceClient;
import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.dto.AdminSecurityOverviewResponse;
import com.cloudnest.auth.dto.AdminUserResponse;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.ChangePasswordRequest;
import com.cloudnest.auth.dto.CreateUserProfileRequest;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginHistoryResponse;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
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
import com.cloudnest.auth.entity.OtpVerification;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.entity.UserSession;
import com.cloudnest.auth.exception.AccountDisabledException;
import com.cloudnest.auth.exception.AccountLockedException;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.exception.EmailNotVerifiedException;
import com.cloudnest.auth.exception.InvalidRefreshTokenException;
import com.cloudnest.auth.exception.OtpException;
import com.cloudnest.auth.exception.SessionNotFoundException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.mapper.UserMapper;
import com.cloudnest.auth.repository.LoginHistoryRepository;
import com.cloudnest.auth.repository.SecurityLogRepository;
import com.cloudnest.auth.repository.TrustedDeviceRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.repository.UserSessionRepository;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.service.AccountLockService;
import com.cloudnest.auth.service.EmailService;
import com.cloudnest.auth.service.OtpService;
import com.cloudnest.auth.service.RefreshTokenService;
import com.cloudnest.auth.service.SecurityEventService;
import com.cloudnest.auth.service.SessionService;
import com.cloudnest.auth.service.TrustedDeviceService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of the {@link com.cloudnest.auth.service.AuthService}
 * contract.
 * <p>
 * Orchestrates the enterprise flows:
 * <ul>
 *   <li><b>Registration</b> — account created {@code PENDING_VERIFICATION},
 *       activated only after the emailed OTP is verified.</li>
 *   <li><b>Login</b> — password check, account lock on repeated failures,
 *       then either an OTP challenge or a direct token pair when the device
 *       is trusted and OTP skipping is enabled.</li>
 *   <li><b>Forgot password</b> — OTP → short-lived reset token → new
 *       password, revoking every refresh token and session.</li>
 *   <li><b>Tokens</b> — short-lived access tokens with rotating refresh
 *       tokens; logout / logout-all revoke immediately.</li>
 * </ul>
 */
@Slf4j
@Service
public class AuthServiceImpl implements com.cloudnest.auth.service.AuthService {

    private final UserCredentialRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final SecurityLogRepository securityLogRepository;
    private final UserSessionRepository userSessionRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthProperties properties;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final TrustedDeviceService trustedDeviceService;
    private final AccountLockService lockService;
    private final SecurityEventService securityEvents;
    private final UserServiceClient userServiceClient;

    public AuthServiceImpl(UserCredentialRepository userRepository,
                           LoginHistoryRepository loginHistoryRepository,
                           SecurityLogRepository securityLogRepository,
                           UserSessionRepository userSessionRepository,
                           TrustedDeviceRepository trustedDeviceRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           AuthProperties properties,
                           OtpService otpService,
                           EmailService emailService,
                           RefreshTokenService refreshTokenService,
                           SessionService sessionService,
                           TrustedDeviceService trustedDeviceService,
                           AccountLockService lockService,
                           SecurityEventService securityEvents,
                           UserServiceClient userServiceClient) {
        this.userRepository = userRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.securityLogRepository = securityLogRepository;
        this.userSessionRepository = userSessionRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.properties = properties;
        this.otpService = otpService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
        this.trustedDeviceService = trustedDeviceService;
        this.lockService = lockService;
        this.securityEvents = securityEvents;
        this.userServiceClient = userServiceClient;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Registration (email OTP activation)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request, ClientInfo clientInfo) {
        log.debug("Registering new user: username={}, email={}", request.getUsername(), request.getEmail());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        UserCredential user = UserCredential.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .enabled(true)
                .status(UserCredential.AccountStatus.PENDING_VERIFICATION)
                .failedAttempts(0)
                .build();
        user = userRepository.save(user);

        // ── Provision the user-service profile (best-effort, idempotent) ────
        // The profile powers /users/me, sharing recipients and the admin user
        // list. If the user-service is momentarily unavailable the profile is
        // retried at email verification; a missing profile never blocks auth.
        syncProfileBestEffort(user);

        OtpService.OtpDispatchResult dispatch = otpService.generateAndSend(user, OtpVerification.Purpose.REGISTRATION);

        log.info("User registered (pending verification): id={}, username={}", user.getId(), user.getUsername());

        return RegisterResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .message("Account created. Check your email for the activation code.")
                .devOtp(dispatch.devOtp())
                .resendAfterSeconds(dispatch.resendAfterSeconds())
                .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyRegistration(VerifyOtpRequest request, ClientInfo clientInfo,
                                           String deviceId, boolean rememberDevice) {
        UserCredential user = resolveUserForVerification(request, OtpVerification.Purpose.REGISTRATION);

        otpService.verify(user, OtpVerification.Purpose.REGISTRATION, request.getCode());

        user.setStatus(UserCredential.AccountStatus.ACTIVE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Retry the profile provisioning in case it failed at registration.
        syncProfileBestEffort(user);

        securityEvents.log(user, SecurityEventService.ACTION_ACCOUNT_ACTIVATED, clientInfo,
                "Email verified and account activated");

        emailService.sendWelcome(user.getEmail(), user.getUsername());
        log.info("Account activated: id={}, username={}", user.getId(), user.getUsername());

        return completeLogin(user, clientInfo, deviceId, rememberDevice);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Login
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, ClientInfo clientInfo, String deviceId) {
        log.debug("Login attempt: usernameOrEmail={}", request.getUsernameOrEmail());

        UserCredential user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    // No user id exists to record against — stay silent and generic.
                    log.warn("Login failed: user '{}' not found", request.getUsernameOrEmail());
                    return new BadCredentialsException("Invalid username/email or password");
                });

        // -- Account state checks --------------------------------------------
        if (UserCredential.AccountStatus.PENDING_VERIFICATION.equals(user.getStatus())) {
            throw new EmailNotVerifiedException(
                    "Please verify your email address before signing in. Use the activation code sent to you.");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            securityEvents.recordLogin(user, false, clientInfo, "Account disabled");
            log.warn("Login blocked: account userId={} is disabled", user.getId());
            throw new AccountDisabledException(
                    "This account has been disabled. Contact an administrator.");
        }
        if (lockService.isLocked(user)) {
            long minutes = lockService.remainingMinutes(user);
            securityEvents.recordLogin(user, false, clientInfo, "Account locked");
            log.warn("Login blocked: account userId={} is locked", user.getId());
            throw new AccountLockedException(minutes);
        }

        // -- Password verification -------------------------------------------
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            boolean becameLocked = lockService.registerFailure(user);
            userRepository.save(user);
            securityEvents.recordLogin(user, false, clientInfo, "Invalid password");
            if (becameLocked) {
                securityEvents.sendAccountLockedAlert(user, clientInfo,
                        properties.getLock().getDurationMinutes());
                throw new AccountLockedException(properties.getLock().getDurationMinutes());
            }
            log.warn("Login failed: invalid password for user '{}' (attempt {}/{})",
                    user.getUsername(), user.getFailedAttempts(), properties.getLock().getMaxFailedAttempts());
            throw new BadCredentialsException("Invalid username/email or password");
        }

        // -- Success path ------------------------------------------------------
        lockService.reset(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        securityEvents.recordLogin(user, true, clientInfo, null);
        log.info("Password verified for userId={}", user.getId());

        boolean trusted = trustedDeviceService.isTrusted(user.getId(), deviceId);

        // Trusted device + skip enabled → straight to a token pair.
        if (properties.getSecurity().isSkipOtpOnTrustedDevice() && trusted) {
            AuthResponse auth = completeLogin(user, clientInfo, deviceId, true);
            return toLoginResponse(auth, true, null, null);
        }

        // Otherwise dispatch a login OTP.
        OtpService.OtpDispatchResult dispatch = otpService.generateAndSend(user, OtpVerification.Purpose.LOGIN);
        String challengeToken = jwtProvider.generateChallengeToken(user.getId(), "LOGIN");

        return LoginResponse.builder()
                .requiresOtp(true)
                .challengeToken(challengeToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .devOtp(dispatch.devOtp())
                .resendAfterSeconds(dispatch.resendAfterSeconds())
                .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                .trustedDevice(false)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyLogin(VerifyOtpRequest request, ClientInfo clientInfo,
                                    String deviceId, boolean rememberDevice) {
        UserCredential user = resolveUserForVerification(request, OtpVerification.Purpose.LOGIN);

        otpService.verify(user, OtpVerification.Purpose.LOGIN, request.getCode());
        securityEvents.log(user, SecurityEventService.ACTION_OTP_VERIFIED, clientInfo, "Login OTP verified");

        return completeLogin(user, clientInfo, deviceId, rememberDevice);
    }

    @Override
    @Transactional
    public OtpDispatchResponse resendOtp(ResendOtpRequest request, ClientInfo clientInfo) {
        UserCredential user = resolveUserForResend(request);

        OtpVerification.Purpose purpose;
        if (request.getChallengeToken() != null && !request.getChallengeToken().isBlank()) {
            Claims claims = requireChallengeClaims(request.getChallengeToken(), null);
            String rawPurpose = claims.get("purpose", String.class);
            try {
                purpose = OtpVerification.Purpose.valueOf(rawPurpose);
            } catch (IllegalArgumentException e) {
                throw new com.cloudnest.auth.exception.OtpException("Invalid verification request");
            }
        } else {
            purpose = UserCredential.AccountStatus.PENDING_VERIFICATION.equals(user.getStatus())
                    ? OtpVerification.Purpose.REGISTRATION
                    : OtpVerification.Purpose.LOGIN;
        }

        OtpService.OtpDispatchResult dispatch = otpService.generateAndSend(user, purpose);
        return OtpDispatchResponse.builder()
                .sent(true)
                .challengeToken(request.getChallengeToken())
                .devOtp(dispatch.devOtp())
                .resendAfterSeconds(dispatch.resendAfterSeconds())
                .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Forgot password
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OtpDispatchResponse forgotPassword(ForgotPasswordRequest request, ClientInfo clientInfo) {
        UserCredential user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Never reveal whether the account exists.
        if (user == null) {
            log.info("Forgot-password requested for unknown email (no action taken)");
            return OtpDispatchResponse.builder()
                    .sent(false)
                    .resendAfterSeconds(properties.getOtp().getResendCooldownSeconds())
                    .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                    .build();
        }

        OtpService.OtpDispatchResult dispatch =
                otpService.generateAndSend(user, OtpVerification.Purpose.PASSWORD_RESET);
        String challengeToken = jwtProvider.generateChallengeToken(user.getId(), "PASSWORD_RESET");

        return OtpDispatchResponse.builder()
                .sent(true)
                .challengeToken(challengeToken)
                .devOtp(dispatch.devOtp())
                .resendAfterSeconds(dispatch.resendAfterSeconds())
                .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                .build();
    }

    @Override
    @Transactional
    public ResetTokenResponse verifyForgotPasswordOtp(VerifyOtpRequest request, ClientInfo clientInfo) {
        UserCredential user = resolveUserForVerification(request, OtpVerification.Purpose.PASSWORD_RESET);

        otpService.verify(user, OtpVerification.Purpose.PASSWORD_RESET, request.getCode());

        String resetToken = jwtProvider.generateChallengeToken(user.getId(), "PASSWORD_RESET_CONFIRMED");
        return ResetTokenResponse.builder().resetToken(resetToken).build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request, ClientInfo clientInfo) {
        Claims claims = requireChallengeClaims(request.getResetToken(), "PASSWORD_RESET_CONFIRMED");

        Long userId = claims.get("userId", Long.class);
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid reset token"));

        // Prevent re-using the current password.
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setStatus(UserCredential.AccountStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        userRepository.save(user);

        // Force every device to re-authenticate.
        refreshTokenService.revokeAllForUser(userId);
        sessionService.endAll(userId);

        securityEvents.log(user, SecurityEventService.ACTION_PASSWORD_RESET, clientInfo,
                "Password reset via OTP; all sessions ended");
        emailService.sendPasswordResetConfirmation(user.getEmail(), user.getUsername());
        securityEvents.notify(user, NotificationServiceClient.TYPE_PASSWORD_RESET,
                "Password reset",
                "Your password was reset through the forgot-password flow. Every other "
                        + "session was signed out for your protection.");
        log.info("Password reset for userId={}", userId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tokens
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AuthResponse validateToken(String token) {
        Claims claims = jwtProvider.validateToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        // Only access tokens are valid at protected endpoints.
        String type = jwtProvider.extractType(claims);
        if (type != null && !"ACCESS".equals(type)) {
            throw new BadCredentialsException("Token type is not valid for this operation");
        }

        Long userId = claims.get("userId", Long.class);
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + userId));

        return UserMapper.toAuthResponse(user, token);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, ClientInfo clientInfo) {
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(rawRefreshToken);
        // The rotate call resolves the user internally; load for the response.
        Claims claims = jwtProvider.validateToken(pair.accessToken()).orElseThrow();
        Long userId = claims.get("userId", Long.class);
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));

        return AuthResponse.builder()
                .token(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .requiresOtp(false)
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId, String sessionId, ClientInfo clientInfo) {
        if (sessionId != null) {
            sessionService.end(sessionId);
            refreshTokenService.revokeAllForSession(sessionId);
        }
        UserCredential user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            securityEvents.log(user, SecurityEventService.ACTION_LOGOUT, clientInfo, "Session ended");
        }
        log.info("Logout for userId={}, sessionId={}", userId, sessionId);
    }

    @Override
    @Transactional
    public void logoutAll(Long userId, ClientInfo clientInfo) {
        int ended = sessionService.endAll(userId);
        refreshTokenService.revokeAllForUser(userId);
        UserCredential user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            securityEvents.log(user, SecurityEventService.ACTION_LOGOUT_ALL, clientInfo,
                    "Logged out from " + ended + " session(s)");
        }
        log.info("Logout-all for userId={} ({} sessions ended)", userId, ended);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Password change (authenticated)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request, ClientInfo clientInfo) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + userId));

        // A locked account must not be usable as a password oracle either.
        if (lockService.isLocked(user)) {
            throw new AccountLockedException(lockService.remainingMinutes(user));
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // Same brute-force protection as sign-in: repeated failures lock.
            boolean becameLocked = lockService.registerFailure(user);
            userRepository.save(user);
            securityEvents.recordLogin(user, false, clientInfo, "Invalid current password");
            if (becameLocked) {
                securityEvents.sendAccountLockedAlert(user, clientInfo,
                        properties.getLock().getDurationMinutes());
                throw new AccountLockedException(properties.getLock().getDurationMinutes());
            }
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        // Verifying the current password is a success signal — reset failures.
        lockService.reset(user);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Access tokens stay valid until they expire; refresh tokens are
        // revoked so every device must re-authenticate soon after.
        refreshTokenService.revokeAllForUser(userId);

        securityEvents.log(user, SecurityEventService.ACTION_PASSWORD_CHANGED, clientInfo,
                "Password changed from settings");
        emailService.sendPasswordChangedAlert(user.getEmail(), user.getUsername(), clientInfo);
        String changedFrom = securityEvents.describeClient(clientInfo);
        String changedPrefix = changedFrom == null
                ? "Your password was changed"
                : "Your password was changed from " + changedFrom;
        securityEvents.notify(user, NotificationServiceClient.TYPE_PASSWORD_CHANGED,
                "Password changed",
                changedPrefix + ". If this wasn't you, contact support immediately.");
        log.info("Password changed for userId={}", userId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Sessions / devices / history
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> getSessions(Long userId, String currentSessionId) {
        return sessionService.listActive(userId).stream()
                .map(session -> toSessionResponse(session, currentSessionId))
                .toList();
    }

    @Override
    @Transactional
    public void endSession(Long userId, String sessionId, ClientInfo clientInfo) {
        UserSession session = sessionService.listActive(userId).stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new SessionNotFoundException("Session not found"));
        sessionService.end(sessionId);
        refreshTokenService.revokeAllForSession(sessionId);
        UserCredential user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            securityEvents.log(user, SecurityEventService.ACTION_SESSION_ENDED, clientInfo,
                    "Session ended remotely: " + session.getDeviceName());
        }
        log.info("Session {} ended for userId={}", sessionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrustedDeviceResponse> getTrustedDevices(Long userId) {
        return trustedDeviceService.list(userId).stream()
                .map(device -> TrustedDeviceResponse.builder()
                        .id(device.getId())
                        .deviceId(device.getDeviceId())
                        .deviceName(device.getDeviceName())
                        .browser(device.getBrowser())
                        .os(device.getOs())
                        .ipAddress(device.getIpAddress())
                        .lastUsedAt(device.getLastUsedAt())
                        .createdAt(device.getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void removeTrustedDevice(Long userId, Long trustedDeviceId, ClientInfo clientInfo) {
        trustedDeviceService.removeById(userId, trustedDeviceId);
        UserCredential user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            securityEvents.log(user, SecurityEventService.ACTION_DEVICE_UNTRUSTED, clientInfo,
                    "Trusted device removed (id=" + trustedDeviceId + ")");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getLoginHistory(Long userId, int page, int size) {
        return loginHistoryRepository.findByUserIdOrderByLoginTimeDesc(userId, PageRequest.of(page, size))
                .map(entry -> LoginHistoryResponse.builder()
                        .id(entry.getId())
                        .success(Boolean.TRUE.equals(entry.getSuccess()))
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
    public Page<SecurityLogResponse> getSecurityLogs(Long userId, int page, int size) {
        return securityEvents.pageLogs(userId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityOverviewResponse getSecurityOverview(Long userId) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + userId));

        long failedLast7Days = loginHistoryRepository
                .countByUserIdAndSuccessFalseAndLoginTimeAfter(userId, LocalDateTime.now().minusDays(7));
        long totalLogins = loginHistoryRepository.countByUserId(userId);

        int score = computeSecurityScore(user, failedLast7Days);

        return SecurityOverviewResponse.builder()
                .securityScore(score)
                .accountStatus(user.getStatus() == null ? "ACTIVE" : user.getStatus().name())
                .emailVerified(user.getEmailVerifiedAt() != null)
                .twoFactorEnabled(false)
                .passwordChangedAt(user.getPasswordChangedAt())
                .lastLoginAt(user.getLastLoginAt())
                .activeSessionCount(sessionService.listActive(userId).size())
                .trustedDeviceCount(trustedDeviceService.list(userId).size())
                .failedLoginsLast7Days(failedLast7Days)
                .totalLogins(totalLogins)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Admin (platform-wide)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AdminSecurityOverviewResponse getAdminSecurityOverview() {
        long failedLast7Days = loginHistoryRepository
                .countBySuccessFalseAndLoginTimeAfter(LocalDateTime.now().minusDays(7));

        return AdminSecurityOverviewResponse.builder()
                .totalAccounts(userRepository.count())
                .activeAccounts(userRepository.countByStatus(UserCredential.AccountStatus.ACTIVE))
                .lockedAccounts(userRepository.countByStatus(UserCredential.AccountStatus.LOCKED))
                .pendingVerification(userRepository.countByStatus(UserCredential.AccountStatus.PENDING_VERIFICATION))
                .disabledAccounts(userRepository.countByEnabled(false))
                .adminCount(userRepository.countByRole("ROLE_ADMIN"))
                .totalLogins(loginHistoryRepository.count())
                .failedLoginsLast7Days(failedLast7Days)
                .activeSessions(userSessionRepository.countByActiveTrue())
                .trustedDeviceCount(trustedDeviceRepository.count())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponse> getAdminLoginHistory(int page, int size) {
        return loginHistoryRepository.findAllByOrderByLoginTimeDesc(PageRequest.of(page, size))
                .map(entry -> LoginHistoryResponse.builder()
                        .id(entry.getId())
                        .userId(entry.getUserId())
                        .success(Boolean.TRUE.equals(entry.getSuccess()))
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
    public Page<SecurityLogResponse> getAdminSecurityLogs(int page, int size) {
        return securityLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(entry -> SecurityLogResponse.builder()
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
    public AdminUserResponse setUserEnabled(Long targetUserId, boolean enabled,
                                            Long adminId, ClientInfo clientInfo) {
        UserCredential target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + targetUserId));

        if (target.getId().equals(adminId)) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }

        target.setEnabled(enabled);
        userRepository.save(target);

        if (enabled) {
            securityEvents.log(target, SecurityEventService.ACTION_ACCOUNT_ENABLED, clientInfo,
                    "Account re-enabled by administrator");
        } else {
            // Force re-authentication everywhere and log out every device.
            sessionService.endAll(targetUserId);
            refreshTokenService.revokeAllForUser(targetUserId);
            securityEvents.log(target, SecurityEventService.ACTION_ACCOUNT_DISABLED, clientInfo,
                    "Account disabled by administrator; all sessions ended");
        }

        log.info("Admin {} set enabled={} for userId={}", adminId, enabled, targetUserId);
        return toAdminUserResponse(target);
    }

    @Override
    @Transactional
    public AdminUserResponse setUserRole(Long targetUserId, String role,
                                         Long adminId, ClientInfo clientInfo) {
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_USER".equals(role)) {
            throw new IllegalArgumentException("Role must be ROLE_ADMIN or ROLE_USER");
        }

        UserCredential target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for id: " + targetUserId));

        if (target.getId().equals(adminId)) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        target.setRole(role);
        userRepository.save(target);
        securityEvents.log(target, SecurityEventService.ACTION_ROLE_CHANGED, clientInfo,
                "Role changed to " + role + " by administrator");

        log.info("Admin {} set role={} for userId={}", adminId, role, targetUserId);
        return toAdminUserResponse(target);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Provisions (or re-provisions) the user's profile in the User Service.
     * Best-effort: failures are logged and never break the auth flow.
     */
    private void syncProfileBestEffort(UserCredential user) {
        try {
            userServiceClient.createProfile(CreateUserProfileRequest.builder()
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .enabled(user.getEnabled())
                    .build());
        } catch (Exception e) {
            log.warn("User profile sync to user-service failed for userId={} (will retry): {}",
                    user.getId(), e.getMessage());
        }
    }

    /**
     * Maps a credential into the admin mutation response DTO.
     */
    private AdminUserResponse toAdminUserResponse(UserCredential user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .status(user.getStatus() == null ? null : user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Completes a sign-in: creates a session, issues the token pair, records
     * the security events and raises new/unknown-device alert emails.
     */
    private AuthResponse completeLogin(UserCredential user, ClientInfo clientInfo,
                                       String deviceId, boolean rememberDevice) {
        // Re-check the lock: the account may have been locked between the OTP
        // being issued and the code being submitted.
        if (lockService.isLocked(user)) {
            throw new AccountLockedException(lockService.remainingMinutes(user));
        }

        // Self-healing profile sync: covers accounts created before the
        // provisioning Feign call existed (and any earlier best-effort miss).
        // Idempotent on the user-service side; never blocks the sign-in.
        syncProfileBestEffort(user);

        // Detect unknown devices BEFORE the session is recorded — otherwise
        // the session created below would make every device look "known".
        boolean knownDevice = securityEvents.isKnownDevice(user, clientInfo);

        boolean trusted = rememberDevice || trustedDeviceService.isTrusted(user.getId(), deviceId);
        if (rememberDevice && deviceId != null && !deviceId.isBlank()) {
            trustedDeviceService.markTrusted(user, clientInfo);
            securityEvents.log(user, SecurityEventService.ACTION_DEVICE_TRUSTED, clientInfo,
                    "Device remembered on sign-in");
        }

        String sessionId = sessionService.create(user, clientInfo, trusted);
        RefreshTokenService.TokenPair pair = refreshTokenService.issue(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole(), sessionId);

        securityEvents.sendLoginAlert(user, clientInfo, knownDevice);
        securityEvents.log(user, SecurityEventService.ACTION_LOGIN_SUCCESS, clientInfo, "Signed in");

        return AuthResponse.builder()
                .token(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .requiresOtp(false)
                .build();
    }

    private LoginResponse toLoginResponse(AuthResponse auth, boolean trusted, String devOtp, Long resendAfter) {
        return LoginResponse.builder()
                .requiresOtp(false)
                .token(auth.getToken())
                .refreshToken(auth.getRefreshToken())
                .userId(auth.getUserId())
                .username(auth.getUsername())
                .email(auth.getEmail())
                .role(auth.getRole())
                .devOtp(devOtp)
                .resendAfterSeconds(resendAfter)
                .otpExpiryMinutes(properties.getOtp().getExpiryMinutes())
                .trustedDevice(trusted)
                .build();
    }

    private SessionResponse toSessionResponse(UserSession session, String currentSessionId) {
        return SessionResponse.builder()
                .sessionId(session.getSessionId())
                .deviceId(session.getDeviceId())
                .deviceName(session.getDeviceName())
                .browser(session.getBrowser())
                .os(session.getOs())
                .deviceType(session.getDeviceType())
                .ipAddress(session.getIpAddress())
                .location(session.getLocation())
                .current(session.getSessionId().equals(currentSessionId))
                .trusted(Boolean.TRUE.equals(session.getTrusted()))
                .loginTime(session.getLoginTime())
                .lastActive(session.getLastActive())
                .build();
    }

    /**
     * Resolves the user for an OTP verification: from the challenge token
     * (login / password-reset) or from the email address (registration).
     */
    private UserCredential resolveUserForVerification(VerifyOtpRequest request,
                                                      OtpVerification.Purpose expectedPurpose) {
        if (request.getChallengeToken() != null && !request.getChallengeToken().isBlank()) {
            Claims claims = requireChallengeClaims(request.getChallengeToken(), expectedPurpose.name());
            Long userId = claims.get("userId", Long.class);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new OtpException("User not found"));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new OtpException("User not found"));
        }
        throw new OtpException("Either an email address or a challenge token is required");
    }

    private UserCredential resolveUserForResend(ResendOtpRequest request) {
        if (request.getChallengeToken() != null && !request.getChallengeToken().isBlank()) {
            Claims claims = requireChallengeClaims(request.getChallengeToken(), null);
            Long userId = claims.get("userId", Long.class);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new OtpException("User not found"));
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            return userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new OtpException("User not found"));
        }
        throw new OtpException("Either an email address or a challenge token is required");
    }

    /**
     * Validates a challenge JWT and (optionally) its purpose.
     */
    private Claims requireChallengeClaims(String token, String expectedPurpose) {
        Claims claims = jwtProvider.validateToken(token)
                .orElseThrow(() -> new OtpException("This verification session has expired. Please start again."));
        if (!"CHALLENGE".equals(jwtProvider.extractType(claims))) {
            throw new OtpException("Invalid verification token");
        }
        if (expectedPurpose != null) {
            String purpose = claims.get("purpose", String.class);
            if (!expectedPurpose.equals(purpose)) {
                throw new OtpException("Invalid verification token for this step");
            }
        }
        return claims;
    }

    /**
     * Heuristic 0–100 security score. Signal-based and documented so it can
     * be tuned or replaced with a risk engine later.
     */
    private int computeSecurityScore(UserCredential user, long failedLoginsLast7Days) {
        if (user.getEmailVerifiedAt() == null) {
            return 0;
        }
        int score = 40;

        // Email verified.
        score += 20;

        // Password age: fresh passwords are stronger signals.
        LocalDateTime changed = user.getPasswordChangedAt();
        boolean passwordRecentlyChanged = changed != null
                && changed.isAfter(LocalDateTime.now().minusDays(90));
        score += passwordRecentlyChanged ? 20 : 5;

        // Fewer trusted devices = smaller attack surface.
        long trustedCount = trustedDeviceService.list(user.getId()).size();
        score += trustedCount <= 3 ? 10 : 5;

        // Recent brute-force attempts lower the score.
        score -= Math.min(20, failedLoginsLast7Days * 4);

        return Math.max(0, Math.min(100, score));
    }
}
