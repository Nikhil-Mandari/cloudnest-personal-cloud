package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.mapper.UserMapper;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link AuthService} interface.
 * <p>
 * Handles user registration with duplicate validation, login with password
 * verification, and JWT token validation.
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthServiceImpl(UserCredentialRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    /**
     * Registers a new user. Validates that the username and email are not already taken.
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.debug("Registering new user: username={}, email={}", request.getUsername(), request.getEmail());

        // -- Check for duplicates ------------------------------------------------
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: username '{}' already taken", request.getUsername());
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email '{}' already registered", request.getEmail());
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        // -- Build and persist the user entity -----------------------------------
        UserCredential user = UserCredential.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: id={}, username={}", user.getId(), user.getUsername());

        // -- Generate JWT -------------------------------------------------------
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        return UserMapper.toAuthResponse(user, token);
    }

    /**
     * Authenticates a user by username/email and password.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.debug("Login attempt: usernameOrEmail={}", request.getUsernameOrEmail());

        // -- Resolve user by username or email ------------------------------------
        UserCredential user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> {
                    log.warn("Login failed: user '{}' not found", request.getUsernameOrEmail());
                    return new UsernameNotFoundException("User not found: " + request.getUsernameOrEmail());
                });

        // -- Verify password ------------------------------------------------------
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for user '{}'", user.getUsername());
            throw new BadCredentialsException("Invalid username/email or password");
        }

        log.info("User logged in successfully: id={}, username={}", user.getId(), user.getUsername());

        // -- Generate JWT ---------------------------------------------------------
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        return UserMapper.toAuthResponse(user, token);
    }

    /**
     * Validates a JWT token and returns the associated user details.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse validateToken(String token) {
        log.debug("Validating JWT token");

        Claims claims = jwtProvider.validateToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired token"));

        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new BadCredentialsException("Token does not contain a valid userId");
        }

        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Token validation failed: user not found for id={}", userId);
                    return new UsernameNotFoundException("User not found for id: " + userId);
                });

        log.debug("Token validated successfully for user: id={}, username={}", user.getId(), user.getUsername());

        return UserMapper.toAuthResponse(user, token);
    }
}
