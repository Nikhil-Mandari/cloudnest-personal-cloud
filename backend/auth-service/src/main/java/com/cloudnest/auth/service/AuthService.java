package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AdminSecurityOverviewResponse;
import com.cloudnest.auth.dto.AdminUserResponse;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.ChangePasswordRequest;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginHistoryResponse;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;
import com.cloudnest.auth.dto.SecurityLogResponse;
import com.cloudnest.auth.dto.SecurityOverviewResponse;
import com.cloudnest.auth.dto.SessionResponse;
import com.cloudnest.auth.dto.TrustedDeviceResponse;
import com.cloudnest.auth.dto.TwoFactorLoginRequest;
import com.cloudnest.auth.dto.VerifyOtpRequest;
import com.cloudnest.auth.security.ClientInfo;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Authentication contract for the CloudNest Auth Service.
 * <p>
 * Covers registration with email-OTP activation, OTP login with trusted
 * devices, forgot-password via OTP, JWT refresh/rotation/revocation,
 * session & device management, and the security scorecard.
 */
public interface AuthService {

    // ── Registration (email OTP activation) ────────────────────────────────

    /**
     * Registers a user in the {@code PENDING_VERIFICATION} state and emails an
     * activation OTP. No tokens are issued until the email is verified.
     */
    RegisterResponse register(RegisterRequest request, ClientInfo clientInfo);

    /**
     * Verifies the registration OTP, activates the account, and signs the user
     * in (returns a full token pair).
     */
    AuthResponse verifyRegistration(VerifyOtpRequest request, ClientInfo clientInfo,
                                    String deviceId, boolean rememberDevice);

    // ── Login (password → OTP challenge or trusted-device skip) ───────────

    /**
     * Authenticates the password. Returns either a full token pair (trusted
     * device + skip configured) or an OTP challenge to be completed via
     * {@link #verifyLogin}.
     */
    LoginResponse login(LoginRequest request, ClientInfo clientInfo, String deviceId);

    /**
     * Verifies the login OTP and completes the sign-in.
     */
    AuthResponse verifyLogin(VerifyOtpRequest request, ClientInfo clientInfo,
                             String deviceId, boolean rememberDevice);

    /**
     * Completes a sign-in blocked on the 2FA step: verifies the TOTP / backup
     * code bound to the {@code 2FA_LOGIN} challenge token and issues the token
     * pair.
     */
    AuthResponse verifyTwoFactorLogin(TwoFactorLoginRequest request, ClientInfo clientInfo,
                                      String deviceId, boolean rememberDevice);

    /**
     * Completes a sign-in after a successful WebAuthn (passkey) assertion.
     * A valid passkey satisfies both factors, so no OTP/2FA is required.
     */
    AuthResponse completePasskeyLogin(Long userId, ClientInfo clientInfo, String deviceId);

    /**
     * Resends an OTP (cooldown-bounded) for registration, login or password
     * reset.
     */
    OtpDispatchResponse resendOtp(com.cloudnest.auth.dto.ResendOtpRequest request, ClientInfo clientInfo);

    // ── Forgot password (OTP → reset) ─────────────────────────────────────

    /**
     * Starts the password-reset flow. Never reveals whether the email exists.
     */
    OtpDispatchResponse forgotPassword(ForgotPasswordRequest request, ClientInfo clientInfo);

    /**
     * Verifies the password-reset OTP and returns a short-lived reset token.
     */
    ResetTokenResponse verifyForgotPasswordOtp(VerifyOtpRequest request, ClientInfo clientInfo);

    /**
     * Sets the new password, revokes all refresh tokens and ends all sessions.
     */
    void resetPassword(ResetPasswordRequest request, ClientInfo clientInfo);

    // ── Tokens ─────────────────────────────────────────────────────────────

    /**
     * Validates an access token and returns the associated user details.
     */
    AuthResponse validateToken(String token);

    /**
     * Rotates a refresh token and returns a fresh token pair.
     */
    AuthResponse refresh(String rawRefreshToken, ClientInfo clientInfo);

    /**
     * Ends the current session and revokes its refresh tokens.
     */
    void logout(Long userId, String sessionId, ClientInfo clientInfo);

    /**
     * Ends every session for the user and revokes all refresh tokens.
     */
    void logoutAll(Long userId, ClientInfo clientInfo);

    // ── Password change (authenticated) ────────────────────────────────────

    /**
     * Changes the password after verifying the current one. Revokes all
     * refresh tokens so every device must re-authenticate.
     */
    void changePassword(Long userId, ChangePasswordRequest request, ClientInfo clientInfo);

    // ── Sessions / devices / history ───────────────────────────────────────

    /**
     * Lists active sessions, marking the caller's own as {@code current}.
     */
    List<SessionResponse> getSessions(Long userId, String currentSessionId);

    /**
     * Ends a specific session (must belong to the user) and revokes its
     * refresh tokens.
     */
    void endSession(Long userId, String sessionId, ClientInfo clientInfo);

    /**
     * Lists the user's trusted devices.
     */
    List<TrustedDeviceResponse> getTrustedDevices(Long userId);

    /**
     * Removes a trusted device by primary key.
     */
    void removeTrustedDevice(Long userId, Long trustedDeviceId, ClientInfo clientInfo);

    /**
     * Paginated sign-in history.
     */
    Page<LoginHistoryResponse> getLoginHistory(Long userId, int page, int size);

    /**
     * Paginated security log.
     */
    Page<SecurityLogResponse> getSecurityLogs(Long userId, int page, int size);

    /**
     * Aggregated security posture scorecard.
     */
    SecurityOverviewResponse getSecurityOverview(Long userId);

    // ── Admin (platform-wide, ROLE_ADMIN only) ─────────────────────────────

    /**
     * Platform-wide security overview: account mix, login volume, sessions
     * and trusted devices across every user.
     */
    AdminSecurityOverviewResponse getAdminSecurityOverview();

    /**
     * Sign-in history across every user, newest first.
     */
    Page<LoginHistoryResponse> getAdminLoginHistory(int page, int size);

    /**
     * Security log across every user, newest first.
     */
    Page<SecurityLogResponse> getAdminSecurityLogs(int page, int size);

    /**
     * Enables or disables a user account. Disabling ends every session and
     * revokes all refresh tokens so the user is signed out everywhere.
     *
     * @param targetUserId the account to change
     * @param enabled      the new enabled state
     * @param adminId      the acting administrator (cannot change themselves)
     * @param clientInfo   client context for the security log
     * @return the updated credential snapshot
     */
    AdminUserResponse setUserEnabled(Long targetUserId, boolean enabled, Long adminId, ClientInfo clientInfo);

    /**
     * Changes a user's role (ROLE_ADMIN / ROLE_USER).
     *
     * @param targetUserId the account to change
     * @param role         the new role
     * @param adminId      the acting administrator (cannot change themselves)
     * @param clientInfo   client context for the security log
     * @return the updated credential snapshot
     */
    AdminUserResponse setUserRole(Long targetUserId, String role, Long adminId, ClientInfo clientInfo);
}
