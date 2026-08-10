package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after successful registration
 * (before email OTP verification).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {

    private Long userId;
    private String email;
    private String message;

    /**
     * Development-mode plain-text OTP, returned only when SMTP is not
     * configured so frontend flows are not blocked.
     */
    private String devOtp;

    /** Seconds the frontend must wait before allowing a resend. */
    private Integer resendAfterSeconds;

    /** Minutes until the OTP expires. */
    private Integer otpExpiryMinutes;
}