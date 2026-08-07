package com.cloudnest.share.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for verifying the password of a password-protected share link.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifySharePasswordRequest {

    @NotBlank(message = "Password must not be blank")
    private String password;
}
