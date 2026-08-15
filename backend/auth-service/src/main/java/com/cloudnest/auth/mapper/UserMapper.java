package com.cloudnest.auth.mapper;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.entity.UserCredential;

/**
 * Maps between {@link UserCredential} entities and DTOs.
 */
public final class UserMapper {

    private UserMapper() {
        // Utility class — prevent instantiation
    }

    /**
     * Converts a {@link UserCredential} into an {@link AuthResponse} with the given JWT token.
     *
     * @param user  the user entity (must not be null)
     * @param token the JWT token string
     * @return a populated {@link AuthResponse}
     */
    public static AuthResponse toAuthResponse(UserCredential user, String token) {
        return toAuthResponse(user, token, null);
    }

    /**
     * Converts a {@link UserCredential} into an {@link AuthResponse} with the
     * given JWT token and an optional refresh token.
     *
     * @param user         the user entity (must not be null)
     * @param token        the JWT token string
     * @param refreshToken the opaque refresh token (may be null)
     * @return a populated {@link AuthResponse}
     */
    public static AuthResponse toAuthResponse(UserCredential user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
