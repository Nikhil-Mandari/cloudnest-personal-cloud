package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for OTP verification endpoints
 * ({@code /api/auth/register/verify}, {@code /api/auth/login/verify}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {

    /** Short-lived challenge JWT (login / password-reset flows). */
    private String challengeToken;

    /** Email address (registration flow). */
    private String email;

    /** The OTP code entered by the user. */
    @NotBlank(message = "Verification code is required")
    private String code;
}