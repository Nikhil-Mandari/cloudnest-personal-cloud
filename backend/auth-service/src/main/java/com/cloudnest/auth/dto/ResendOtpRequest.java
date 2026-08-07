package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for resending an OTP. Either {@code email} (registration /
 * forgot-password) or {@code challengeToken} (login) identifies the pending
 * verification.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResendOtpRequest {

    private String email;
    private String challengeToken;
}
