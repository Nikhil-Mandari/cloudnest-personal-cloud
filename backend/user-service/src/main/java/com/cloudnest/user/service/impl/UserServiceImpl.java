package com.cloudnest.user.service.impl;

import com.cloudnest.user.dto.AdminUserSummaryResponse;
import com.cloudnest.user.dto.CreateUserProfileRequest;
import com.cloudnest.user.dto.PagedUsersResponse;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.entity.User;
import com.cloudnest.user.exception.DuplicateResourceException;
import com.cloudnest.user.exception.ResourceNotFoundException;
import com.cloudnest.user.mapper.UserMapper;
import com.cloudnest.user.repository.UserRepository;
import com.cloudnest.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link UserService} interface.
 * <p>
 * Handles user profile retrieval, updates, deletion, and search.
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Long userId) {
        log.debug("Fetching current user profile: userId={}", userId);

        User user = findUserById(userId);
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Retrieves a user profile by their unique ID.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long id) {
        log.debug("Fetching user profile by id={}", id);

        User user = findUserById(id);
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Updates the profile of the currently authenticated user.
     */
    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.debug("Updating profile for userId={}", userId);

        User user = findUserById(userId);

        // -- Check email uniqueness if being updated --------------------------------
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Update profile failed: email '{}' already in use", request.getEmail());
                throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
            }
        }

        UserMapper.applyUpdate(user, request);
        user = userRepository.save(user);

        log.info("User profile updated successfully: userId={}", userId);
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Deletes the currently authenticated user's account.
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Deleting user: userId={}", userId);

        User user = findUserById(userId);
        userRepository.delete(user);

        log.info("User deleted successfully: userId={}, username={}", userId, user.getUsername());
    }

    /**
     * Searches for users by username, display name, or email.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String query) {
        log.debug("Searching users with query='{}'", query);

        if (query == null || query.trim().isEmpty()) {
            log.debug("Empty search query — returning all users");
            return userRepository.findAll().stream()
                    .map(UserMapper::toProfileResponse)
                    .collect(Collectors.toList());
        }

        List<User> users = userRepository.search(query.trim());
        log.debug("Search found {} users for query='{}'", users.size(), query);

        return users.stream()
                .map(UserMapper::toProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Idempotently creates (or returns) a user profile.
     */
    @Override
    @Transactional
    public UserProfileResponse createProfile(CreateUserProfileRequest request) {
        log.info("Provisioning user profile: email={}, username={}", request.getEmail(), request.getUsername());

        // -- Idempotency: return the existing profile if already provisioned ------
        User existing = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElse(null);
        if (existing == null && request.getUsername() != null) {
            existing = userRepository.findByUsername(request.getUsername()).orElse(null);
        }
        if (existing != null) {
            log.info("Profile already exists for email={} — returning existing", request.getEmail());
            return UserMapper.toProfileResponse(existing);
        }

        String role = (request.getRole() != null && !request.getRole().isBlank())
                ? request.getRole() : "ROLE_USER";

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail().trim().toLowerCase())
                .displayName(request.getDisplayName())
                .role(role)
                .enabled(request.getEnabled() != null ? request.getEnabled() : Boolean.TRUE)
                .build();

        user = userRepository.save(user);
        log.info("User profile provisioned: userId={}, role={}", user.getId(), role);
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Platform-wide user aggregates for the admin overview.
     */
    @Override
    @Transactional(readOnly = true)
    public AdminUserSummaryResponse getAdminSummary() {
        long total = userRepository.count();
        long disabled = userRepository.countByEnabledFalse();
        long admins = userRepository.countByRole("ROLE_ADMIN");
        long newLast7Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7));

        log.debug("Admin user summary: total={}, disabled={}, admins={}, new7d={}",
                total, disabled, admins, newLast7Days);

        return AdminUserSummaryResponse.builder()
                .totalUsers(total)
                .activeUsers(total - disabled)
                .disabledUsers(disabled)
                .adminUsers(admins)
                .newLast7Days(newLast7Days)
                .build();
    }

    /**
     * Paged, filterable listing of all user profiles (admin view).
     */
    @Override
    @Transactional(readOnly = true)
    public PagedUsersResponse listUsersForAdmin(int page, int size, String query) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> result;
        if (query != null && !query.trim().isEmpty()) {
            result = userRepository.searchPage(query.trim(), pageable);
        } else {
            result = userRepository.findAll(pageable);
        }

        List<UserProfileResponse> content = result.getContent().stream()
                .map(UserMapper::toProfileResponse)
                .collect(Collectors.toList());

        return PagedUsersResponse.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Internal helper to find a user by ID or throw {@link ResourceNotFoundException}.
     */
    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
    }
}
