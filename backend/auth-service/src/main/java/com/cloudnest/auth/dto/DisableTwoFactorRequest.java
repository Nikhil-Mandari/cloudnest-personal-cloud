package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Disables 2FA; verification may be a TOTP code, unused backup code or password. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisableTwoFactorRequest {

    @NotBlank(message = "Verification is required")
    private String verification;
}
