package com.cloudnest.user.mapper;

import com.cloudnest.user.dto.CreateProfileRequest;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.entity.User;

/**
 * Maps between {@link User} entities and DTOs.
 */
public final class UserMapper {

    private UserMapper() {
        // Utility class — prevent instantiation
    }

    /**
     * Converts a {@link User} entity into a {@link UserProfileResponse}.
     *
     * @param user the user entity (must not be null)
     * @return a populated {@link UserProfileResponse}
     */
    public static UserProfileResponse toProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Applies provisioning fields from a {@link CreateProfileRequest} to a new
     * {@link User} entity. The Auth Service's numeric user ID is persisted as
     * the profile primary key, and the profile starts enabled.
     *
     * @param user    the user entity to populate (must be a new instance)
     * @param request the provisioning payload
     */
    public static void applyCreate(User user, CreateProfileRequest request) {
        user.setId(request.getId());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setEnabled(true);
    }

    /**
     * Applies non-null fields from an {@link UpdateProfileRequest} to an existing
     * {@link User} entity. This is a partial update — only provided fields are applied.
     *
     * @param user    the user entity to update (mutated in place)
     * @param request the update payload with optional fields
     */
    public static void applyUpdate(User user, UpdateProfileRequest request) {
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
    }
}
