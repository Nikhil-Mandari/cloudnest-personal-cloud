package com.cloudnest.user.service;

import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;

import java.util.List;

/**
 * Service interface for user profile operations.
 */
public interface UserService {

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param userId the authenticated user's ID
     * @return the user's profile
     */
    UserProfileResponse getCurrentUser(Long userId);

    /**
     * Retrieves a user profile by their unique ID.
     *
     * @param id the user ID to look up
     * @return the matching user profile
     */
    UserProfileResponse getUserById(Long id);

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param userId  the authenticated user's ID
     * @param request the profile fields to update
     * @return the updated user profile
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /**
     * Deletes the currently authenticated user's account.
     *
     * @param userId the authenticated user's ID
     */
    void deleteUser(Long userId);

    /**
     * Searches for users by username, display name, or email.
     *
     * @param query the search term
     * @return a list of matching user profiles
     */
    List<UserProfileResponse> searchUsers(String query);
}
