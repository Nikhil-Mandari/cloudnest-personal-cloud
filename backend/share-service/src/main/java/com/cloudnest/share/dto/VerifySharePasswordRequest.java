package com.cloudnest.share.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for verifying a password-protected public share link
 * ({@code POST /api/shares/public/{token}/verify-password}).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifySharePasswordRequest {

    /** The plain-text password supplied by the visitor. */
    private String password;
}
