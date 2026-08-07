package com.cloudnest.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Unified login response.
 * <p>
 * When {@code requiresOtp} is {@code true} only {@code challengeToken} (plus
 * user identity) is populated — the caller must complete the OTP step. When
 * {@code false} the full token pair is returned.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    /** {@code true} when the caller must verify an emailed OTP next. */
    @Builder.Default
    private boolean requiresOtp = false;

    /** {@code true} when the caller must verify a TOTP/backup code next (2FA). */
    @Builder.Default
    private boolean requires2fa = false;

    /** Short-lived JWT carrying the pending sign-in (OTP / 2FA step). */
    private String challengeToken;

    /** Access token — present only when {@code requiresOtp} is {@code false}. */
    private String token;

    /** Rotating refresh token — present only when {@code requiresOtp} is false. */
    private String refreshToken;

    private Long userId;
    private String username;
    private String email;
    private String role;

    /** Dev-only plain OTP when email delivery is disabled. */
    private String devOtp;

    /** Seconds until a resend is permitted. */
    private Long resendAfterSeconds;

    /** Minutes before the OTP expires. */
    private Integer otpExpiryMinutes;

    /** Whether the sign-in came from a trusted device. */
    private boolean trustedDevice;
}
