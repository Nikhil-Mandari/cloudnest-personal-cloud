package com.cloudnest.user.service;

import com.cloudnest.user.dto.AdminUserSummaryResponse;
import com.cloudnest.user.dto.CreateUserProfileRequest;
import com.cloudnest.user.dto.PagedUsersResponse;
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

    /**
     * Idempotently creates (or returns) a user profile. Called by the Auth
     * Service after registration and by the admin bootstrap. If a profile
     * already exists for the email or username, the existing record is
     * returned unchanged.
     *
     * @param request the provisioning payload
     * @return the created (or existing) user profile
     */
    UserProfileResponse createProfile(CreateUserProfileRequest request);

    /**
     * Platform-wide user aggregates for the admin overview.
     *
     * @return aggregate user statistics
     */
    AdminUserSummaryResponse getAdminSummary();

    /**
     * Paged, filterable listing of all user profiles (admin view).
     *
     * @param page  zero-based page index
     * @param size  page size
     * @param query optional username/display-name/email filter
     * @return the paged user list
     */
    PagedUsersResponse listUsersForAdmin(int page, int size, String query);
}
