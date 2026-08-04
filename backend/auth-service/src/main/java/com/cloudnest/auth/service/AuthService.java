package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.RegisterRequest;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user account.
     *
     * @param request the registration details (username, email, password)
     * @return an {@link AuthResponse} containing the JWT token and user details
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request the login credentials (username/email and password)
     * @return an {@link AuthResponse} containing the JWT token and user details
     */
    AuthResponse login(LoginRequest request);

    /**
     * Validates a JWT token and returns the associated user details.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return an {@link AuthResponse} containing the token and user details,
     *         or throws an exception if the token is invalid
     */
    AuthResponse validateToken(String token);
}
