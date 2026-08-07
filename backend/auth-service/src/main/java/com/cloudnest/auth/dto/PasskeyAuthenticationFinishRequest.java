package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Passkey sign-in finish: the original assertion request JSON plus the
 * browser's credential response JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationFinishRequest {

    @NotBlank(message = "requestJson is required")
    private String requestJson;

    @NotBlank(message = "responseJson is required")
    private String responseJson;
}
