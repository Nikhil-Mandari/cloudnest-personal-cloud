package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after dispatching an OTP
 * ({@code /api/auth/otp/resend} and the initial register/login flow).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpDispatchResponse {

    /** Whether the email was sent (false in development mode). */
    private boolean sent;

    /** Challenge token for login / 2FA flows. */
    private String challengeToken;

    /** Development-mode plain-text OTP (null in production). */
    private String devOtp;

    /** Seconds before the user can request a new OTP. */
    private Integer resendAfterSeconds;

    /** Minutes until the OTP expires. */
    private Integer otpExpiryMinutes;
}