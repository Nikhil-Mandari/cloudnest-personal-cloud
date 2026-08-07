package com.cloudnest.user.controller;

import com.cloudnest.user.dto.AdminUserSummaryResponse;
import com.cloudnest.user.dto.CreateUserProfileRequest;
import com.cloudnest.user.dto.PagedUsersResponse;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.service.UserService;
import com.cloudnest.user.util.AdminGuard;
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
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @return 200 OK with the current user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<StandardResponse<UserProfileResponse>> getCurrentUser(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/users/me - userId={}", userIdHeader);

        UserProfileResponse profile = userService.getCurrentUser(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("Current user profile retrieved successfully")
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

    /**
     * Provisioning endpoint used by the Auth Service (Feign, direct call) after
     * registration and by the admin bootstrap. Idempotent.
     *
     * @param roleHeader   caller role (set by gateway from JWT, or by the Auth
     *                     Service's Feign interceptor for internal calls)
     * @param request      the provisioning payload
     * @return 200 OK with the created (or existing) profile
     */
    @PostMapping
    public ResponseEntity<StandardResponse<UserProfileResponse>> createProfile(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Valid @RequestBody CreateUserProfileRequest request,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("POST /api/users - provisioning profile for email={}", request.getEmail());

        UserProfileResponse profile = userService.createProfile(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<UserProfileResponse>builder()
                        .success(true)
                        .message("User profile provisioned successfully")
                        .data(profile)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Platform-wide user aggregates for the admin dashboard (admin only).
     *
     * @param roleHeader caller role (set by gateway from JWT)
     * @return 200 OK with aggregate user statistics
     */
    @GetMapping("/admin/summary")
    public ResponseEntity<StandardResponse<AdminUserSummaryResponse>> getAdminSummary(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/users/admin/summary");

        AdminUserSummaryResponse summary = userService.getAdminSummary();

        return ResponseEntity.ok(
                StandardResponse.<AdminUserSummaryResponse>builder()
                        .success(true)
                        .message("Admin user summary retrieved successfully")
                        .data(summary)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Paged, filterable user listing for the admin dashboard (admin only).
     *
     * @param roleHeader caller role (set by gateway from JWT)
     * @param page       zero-based page index
     * @param size       page size
     * @param query      optional username/display-name/email filter
     * @return 200 OK with the paged user list
     */
    @GetMapping("/admin")
    public ResponseEntity<StandardResponse<PagedUsersResponse>> listUsersForAdmin(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "") String query,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/users/admin?page={}&size={}&query={}", page, size, query);

        PagedUsersResponse result = userService.listUsersForAdmin(page, size, query);

        return ResponseEntity.ok(
                StandardResponse.<PagedUsersResponse>builder()
                        .success(true)
                        .message("Users retrieved successfully")
                        .data(result)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
