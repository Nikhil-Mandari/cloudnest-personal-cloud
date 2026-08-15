package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response returned after {@code POST /api/auth/forgot-password/verify}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetTokenResponse {

    /** Short-lived JWT granting permission to set a new password. */
    private String resetToken;
}