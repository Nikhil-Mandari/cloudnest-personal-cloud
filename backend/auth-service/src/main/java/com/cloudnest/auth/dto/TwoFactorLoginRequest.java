package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Completes a sign-in blocked on the 2FA step with a TOTP or backup code. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorLoginRequest {

    /** Short-lived challenge JWT issued by the login attempt. */
    @NotBlank(message = "Challenge token is required")
    private String challengeToken;

    @NotBlank(message = "Code is required")
    private String code;
}
