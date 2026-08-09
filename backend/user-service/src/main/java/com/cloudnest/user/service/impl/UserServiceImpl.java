package com.cloudnest.user.service.impl;

import com.cloudnest.user.dto.CreateUserRequest;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.entity.User;
import com.cloudnest.user.exception.DuplicateResourceException;
import com.cloudnest.user.exception.ResourceNotFoundException;
import com.cloudnest.user.mapper.UserMapper;
import com.cloudnest.user.repository.UserRepository;
import com.cloudnest.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Creates a new user profile.
     */
    @Override
    @Transactional
    public UserProfileResponse createUser(CreateUserRequest request) {
        log.debug("Creating user profile: id={}, username='{}', email='{}'",
                request.getId(), request.getUsername(), request.getEmail());

        // -- Check for duplicates --------------------------------------------------
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Create user failed: username '{}' already exists", request.getUsername());
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Create user failed: email '{}' already exists", request.getEmail());
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .id(request.getId())
                .username(request.getUsername())
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .role(request.getRole() != null ? request.getRole() : "ROLE_USER")
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User profile created successfully: id={}, username='{}'",
                saved.getId(), saved.getUsername());

        return UserMapper.toProfileResponse(saved);
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
