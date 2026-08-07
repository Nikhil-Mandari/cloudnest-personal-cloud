package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to enable TOTP 2FA: the code from the authenticator app proving the
 * user has actually scanned the secret shown during setup.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnableTwoFactorRequest {

    @NotBlank(message = "Authenticator code is required")
    private String code;
}
