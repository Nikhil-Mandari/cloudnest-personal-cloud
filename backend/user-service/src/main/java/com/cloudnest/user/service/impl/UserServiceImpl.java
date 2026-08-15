package com.cloudnest.user.service.impl;

import com.cloudnest.user.dto.CreateProfileRequest;
import com.cloudnest.user.dto.UpdateProfileRequest;
import com.cloudnest.user.dto.UserProfileResponse;
import com.cloudnest.user.entity.User;
import com.cloudnest.user.exception.DuplicateResourceException;
import com.cloudnest.user.exception.ResourceNotFoundException;
import com.cloudnest.user.mapper.UserMapper;
import com.cloudnest.user.repository.UserRepository;
import com.cloudnest.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
     * Creates a user profile using the Auth Service's numeric user ID.
     * <p>
     * Idempotent: if a profile with the given ID already exists it is
     * returned unchanged. Duplicate username/email on a different ID are
     * rejected so retried provisioning can never create duplicates.
     */
    @Override
    @Transactional
    public UserProfileResponse createProfile(CreateProfileRequest request) {
        log.info("Provisioning profile: userId={}, username={}, email={}",
                request.getId(), request.getUsername(), request.getEmail());

        // -- Idempotent: profile already exists --------------------------------
        if (userRepository.existsById(request.getId())) {
            User existing = userRepository.findById(request.getId()).orElseThrow();
            log.info("Profile already exists for userId={} — returning existing", request.getId());
            return UserMapper.toProfileResponse(existing);
        }

        // -- Uniqueness guards (retry / partial-write safety) ------------------
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Provisioning failed: username '{}' already in use", request.getUsername());
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Provisioning failed: email '{}' already in use", request.getEmail());
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already in use");
        }

        User user = new User();
        UserMapper.applyCreate(user, request);
        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race: a concurrent provisioning call created this profile (or a
            // conflicting username/email) between our check and insert. Fall
            // back to the existing row so retries stay idempotent.
            log.warn("Provisioning race detected for userId={} — returning existing profile", request.getId());
            return userRepository.findById(request.getId())
                    .map(UserMapper::toProfileResponse)
                    .orElseThrow(() -> e);
        }

        log.info("User profile provisioned successfully: userId={}, username={}",
                user.getId(), user.getUsername());
        return UserMapper.toProfileResponse(user);
    }

    /**
     * Retrieves the profile of the currently authenticated user, lazily
     * creating it from the forwarded identity headers when it is missing
     * (self-healing). Existing profiles are returned untouched.
     */
    @Override
    @Transactional
    public UserProfileResponse getOrCreateCurrentUser(Long userId, String username, String email, String role) {
        log.debug("Fetching current user profile: userId={}", userId);

        return userRepository.findById(userId)
                .map(UserMapper::toProfileResponse)
                .orElseGet(() -> {
                    if (username == null || username.isBlank() || email == null || email.isBlank()) {
                        log.warn("Profile missing for userId={} and identity headers incomplete — cannot self-heal", userId);
                        throw new ResourceNotFoundException("User not found with id: " + userId);
                    }

                    log.warn("Profile missing for userId={} — self-healing from forwarded identity (username={})",
                            userId, username);
                    // Reuses createProfile() so the self-heal path gets the same
                    // idempotency, uniqueness guards and race handling.
                    return createProfile(CreateProfileRequest.builder()
                            .id(userId)
                            .username(username)
                            .email(email)
                            .role(role == null || role.isBlank() ? "ROLE_USER" : role)
                            .build());
                });
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
