package com.cloudnest.user.controller;

import com.cloudnest.user.dto.CreateProfileRequest;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.service.UserService;
import com.cloudnest.user.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user profile operations.
 * <p>
 * Provides endpoints for retrieving, updating, deleting, and searching user profiles.
 * Endpoints that operate on the "current" user expect the caller to supply their
 * user ID (e.g. via a header set by the API Gateway after JWT authentication).
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * <p>
     * Identity is supplied by the API Gateway via forwarded headers. When the
     * profile is missing it is lazily created (self-healing) from those
     * headers — existing profiles are returned unchanged.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param usernameHeader the username forwarded by the gateway (may be absent on direct calls)
     * @param emailHeader    the email forwarded by the gateway (may be absent on direct calls)
     * @param roleHeader     the role forwarded by the gateway (may be absent on direct calls)
     * @return 200 OK with the current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<StandardResponse<UserProfileResponse>> getCurrentUser(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @RequestHeader(value = "X-User-Username", required = false) String usernameHeader,
            @RequestHeader(value = "X-User-Email", required = false) String emailHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/users/me - userId={}", userIdHeader);

        UserProfileResponse profile =
                userService.getOrCreateCurrentUser(userIdHeader, usernameHeader, emailHeader, roleHeader);

        return ResponseEntity.ok(
                StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("Current user profile retrieved successfully")
                        .data(profile)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Provisions a user profile (internal endpoint called by the Auth Service).
     * <p>
     * Idempotent: calling this with an existing user ID returns the existing
     * profile instead of creating a duplicate. The endpoint is not whitelisted
     * in the API Gateway, so it is not reachable externally without a valid JWT.
     *
     * @param request the profile data (auth user ID, username, email, role)
     * @return 200 OK with the provisioned (or existing) profile
     */
    @PostMapping
    public ResponseEntity<StandardResponse<UserProfileResponse>> createProfile(
            @Valid @RequestBody CreateProfileRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/users - provisioning profile: userId={}, username={}",
                request.getId(), request.getUsername());

        UserProfileResponse profile = userService.createProfile(request);

        return ResponseEntity.ok(
                StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User profile provisioned successfully")
                        .data(profile)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves a user profile by their unique ID.
     *
     * @param id the user ID to look up
     * @return 200 OK with the user's profile
     */
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<UserProfileResponse>> getUserById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        log.info("GET /api/users/{}", id);

        UserProfileResponse profile = userService.getUserById(id);

        return ResponseEntity.ok(
                StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User profile retrieved successfully")
                        .data(profile)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param request      the profile fields to update
     * @return 200 OK with the updated user profile
     */
    @PutMapping("/me")
    public ResponseEntity<StandardResponse<UserProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/users/me - userId={}", userIdHeader);

        UserProfileResponse profile = userService.updateProfile(userIdHeader, request);

        return ResponseEntity.ok(
                StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User profile updated successfully")
                        .data(profile)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Deletes the currently authenticated user's account.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @return 200 OK confirming deletion
     */
    @DeleteMapping("/me")
    public ResponseEntity<StandardResponse<Void>> deleteUser(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/users/me - userId={}", userIdHeader);

        userService.deleteUser(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("User account deleted successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Searches for users by username, display name, or email.
     *
     * @param query the search term (optional — returns all users if empty)
     * @return 200 OK with a list of matching user profiles
     */
    @GetMapping
    public ResponseEntity<StandardResponse<List<UserProfileResponse>>> searchUsers(
            @RequestParam(required = false, defaultValue = "") String query,
            HttpServletRequest httpRequest) {

        log.info("GET /api/users?query={}", query);

        List<UserProfileResponse> users = userService.searchUsers(query);

        return ResponseEntity.ok(
                StandardResponse.<List<UserProfileResponse>>builder()
                        .success(true)
                        .message("Users retrieved successfully")
                        .data(users)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
