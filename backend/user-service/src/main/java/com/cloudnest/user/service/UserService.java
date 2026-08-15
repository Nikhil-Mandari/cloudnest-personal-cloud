package com.cloudnest.user.service;

import com.cloudnest.user.dto.CreateProfileRequest;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;

import java.util.List;

/**
 * Service interface for user profile operations.
 */
public interface UserService {

    /**
     * Creates a user profile using the Auth Service's numeric user ID.
     * <p>
     * Idempotent — if a profile with the given ID already exists it is
     * returned unchanged, so retried provisioning never creates duplicates.
     *
     * @param request the profile data (auth user ID, username, email, role)
     * @return the created (or already-existing) profile
     */
    UserProfileResponse createProfile(CreateProfileRequest request);

    /**
     * Retrieves the profile of the currently authenticated user, lazily
     * creating it from the identity headers forwarded by the API Gateway
     * when it is missing (self-healing for profiles that were never
     * provisioned).
     *
     * @param userId   the authenticated user's ID
     * @param username the username forwarded via {@code X-User-Username} (may be null)
     * @param email    the email forwarded via {@code X-User-Email} (may be null)
     * @param role     the role forwarded via {@code X-User-Role} (may be null)
     * @return the user's profile
     */
    UserProfileResponse getOrCreateCurrentUser(Long userId, String username, String email, String role);

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
