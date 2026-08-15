package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for resending an OTP ({@code /api/auth/otp/resend}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendOtpRequest {

    /** Email address (registration / password-reset flow). */
    private String email;

    /** Challenge token (login / 2FA flow). */
    private String challengeToken;
}