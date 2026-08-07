package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to disable 2FA. {@code verification} must be one of: the current
 * TOTP code, an unused backup code, or the account password — proving the
 * caller controls the account before the second factor is removed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisableTwoFactorRequest {

    @NotBlank(message = "A verification code is required")
    private String verification;
}
