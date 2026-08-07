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
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
