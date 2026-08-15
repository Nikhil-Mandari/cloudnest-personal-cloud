package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after a login attempt.
 * <p>
 * When {@code requiresOtp} is true, the caller must complete the OTP
 * verification step ({@code /api/auth/login/verify}) with the returned
 * {@code challengeToken} before receiving a JWT. When {@code requires2fa}
 * is true, the TOTP / backup-code step is required.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    /** Whether an email OTP step is required before completing login. */
    @Builder.Default
    private boolean requiresOtp = false;

    /** Whether a TOTP / backup-code step is required. */
    @Builder.Default
    private boolean requires2fa = false;

    /** Opaque challenge token binding the login attempt to its OTP. */
    private String challengeToken;

    /** JWT token (present only when no OTP or 2FA is required). */
    private String token;

    /** Refresh token (present when token is issued). */
    private String refreshToken;

    // ── Identity fields (present on initial login attempt) ─────────────────
    private Long userId;
    private String username;
    private String email;
    private String role;

    /** Development-mode OTP, returned when SMTP is not configured. */
    private String devOtp;

    /** Resend cooldown in seconds (for the frontend countdown). */
    private Integer resendAfterSeconds;

    /** OTP expiry window in minutes. */
    private Integer otpExpiryMinutes;

    /** Whether the current device is trusted (skips OTP on subsequent logins). */
    @Builder.Default
    private boolean trustedDevice = false;
}