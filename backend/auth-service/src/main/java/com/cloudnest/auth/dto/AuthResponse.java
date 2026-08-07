package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response payload returned after successful authentication.
 * <p>
 * {@code requiresOtp} stays {@code false} for a completed sign-in; the field
 * exists so clients can distinguish a full auth response from an OTP
 * challenge without inspecting token presence.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    /** Rotating refresh token issued alongside the access token. */
    private String refreshToken;

    private Long userId;
    private String username;
    private String email;
    private String role;

    @Builder.Default
    private boolean requiresOtp = false;
}
