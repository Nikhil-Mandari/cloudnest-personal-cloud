package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for verifying a one-time passcode.
 * <p>
 * Either {@code challengeToken} (login / password-reset flows) or
 * {@code email} (registration flow) identifies the user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {

    /** Short-lived challenge JWT issued by login / forgot-password. */
    private String challengeToken;

    /** Email address — used by the registration activation flow. */
    private String email;

    @NotBlank(message = "Verification code is required")
    private String code;
}
