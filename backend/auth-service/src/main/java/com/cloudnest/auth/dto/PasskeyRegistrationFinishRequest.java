package com.cloudnest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registration ceremony finish: the same creation options the browser used
 * plus the browser's credential response JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyRegistrationFinishRequest {

    @NotBlank(message = "optionsJson is required")
    private String optionsJson;

    @NotBlank(message = "responseJson is required")
    private String responseJson;

    /** Optional user-friendly label for the new credential. */
    private String nickname;
}
