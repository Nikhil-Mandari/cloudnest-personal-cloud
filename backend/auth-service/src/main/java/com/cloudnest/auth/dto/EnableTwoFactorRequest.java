package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Enables 2FA by confirming a TOTP code generated from the setup secret. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnableTwoFactorRequest {

    @NotBlank(message = "Code is required")
    private String code;
}
