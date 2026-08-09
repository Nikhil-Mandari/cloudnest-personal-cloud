package com.cloudnest.auth.controller;

import com.cloudnest.auth.client.UserServiceClient;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.CreateProfileRequest;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.service.AuthService;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 * <p>
 * Provides endpoints for user registration, login, and JWT token validation.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserServiceClient userServiceClient;

    public AuthController(AuthService authService, UserServiceClient userServiceClient) {
        this.authService = authService;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Registers a new user account.
     *
     * @param request the registration payload (username, email, password)
     * @return 201 Created with the JWT token and user details
     */
    @PostMapping("/register")
    public ResponseEntity<StandardResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/register - username={}", request.getUsername());

        // register() is transactional — it has committed by the time it returns,
        // so the remote provisioning call below never holds the Auth DB transaction.
        AuthResponse authResponse = authService.register(request);

        provisionProfile(authResponse);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("User registered successfully")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Best-effort profile provisioning: creates the matching profile in the
     * User Service ({@code user_db.users}) using the same numeric user ID.
     * <p>
     * Registration must NOT fail when the User Service is unavailable, so any
     * failure is logged and swallowed — the missing profile is healed lazily
     * by {@code GET /api/users/me} the next time it is requested.
     *
     * @param authResponse the successful registration result
     */
    private void provisionProfile(AuthResponse authResponse) {
        try {
            CreateProfileRequest profile = CreateProfileRequest.builder()
                    .id(authResponse.getUserId())
                    .username(authResponse.getUsername())
                    .email(authResponse.getEmail())
                    .role(authResponse.getRole())
                    .build();
            userServiceClient.createProfile(profile);
            log.info("User profile provisioned via user-service: userId={}", authResponse.getUserId());
        } catch (Exception e) {
            log.warn("Profile provisioning skipped/failed for userId={} — registration continues: {}",
                    authResponse.getUserId(), e.getMessage());
        }
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login payload (username/email and password)
     * @return 200 OK with the JWT token and user details
     */
    @PostMapping("/login")
    public ResponseEntity<StandardResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/login - usernameOrEmail={}", request.getUsernameOrEmail());

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Validates a JWT token.
     * <p>
     * Expects the token in the {@code Authorization: Bearer <token>} header.
     *
     * @return 200 OK with the validated token and user details
     */
    @GetMapping("/validate")
    public ResponseEntity<StandardResponse<AuthResponse>> validate(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("GET /api/auth/validate - missing or invalid Authorization header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(StandardResponse.<AuthResponse>builder()
                            .success(false)
                            .message("Missing or invalid Authorization header")
                            .path(httpRequest.getRequestURI())
                            .build());
        }

        String token = authHeader.substring(7);
        log.debug("GET /api/auth/validate - validating token");

        AuthResponse authResponse = authService.validateToken(token);

        return ResponseEntity.ok(
                StandardResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Token is valid")
                        .data(authResponse)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
