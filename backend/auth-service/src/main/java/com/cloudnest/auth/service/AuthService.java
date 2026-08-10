package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user account (OTP verification pending).
     *
     * @param request the registration details (username, email, password)
     * @return a {@link RegisterResponse} with OTP dispatch info (no JWT)
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Completes registration by verifying the emailed OTP code.
     *
     * @param email the email address used during registration
     * @param code  the OTP code entered by the user
     * @return an {@link AuthResponse} containing the JWT token and user details
     */
    AuthResponse verifyRegistration(String email, String code);

    /**
     * Resends an OTP. The purpose is derived from the presence of a challenge
     * token: with one it is a LOGIN / PASSWORD_RESET challenge, without one
     * it is the REGISTRATION flow.
     *
     * @param email          the email address to resend the OTP to
     * @param challengeToken optional challenge UUID (login / password-reset)
     * @return an {@link OtpDispatchResponse} with new OTP info
     */
    OtpDispatchResponse resendOtp(String email, String challengeToken);

    /**
     * Authenticates a user and returns a JWT, or an OTP challenge if required.
     *
     * @param request the login credentials (username/email and password)
     * @return a {@link LoginResponse} with either a JWT or an OTP challenge
     */
    LoginResponse login(LoginRequest request);

    /**
     * Completes login by verifying the emailed OTP code.
     *
     * @param challengeToken the challenge UUID from the initial login response
     * @param code           the OTP code entered by the user
     * @return an {@link AuthResponse} containing the JWT token and user details
     */
    AuthResponse verifyLogin(String challengeToken, String code);

    /**
     * Rotates a refresh token into a fresh access-token pair.
     *
     * @param refreshToken the opaque refresh token from the client
     * @return an {@link AuthResponse} with a new JWT and rotated refresh token
     */
    AuthResponse refreshToken(String refreshToken);

    /**
     * Revokes a refresh token (logout).
     *
     * @param refreshToken the refresh token to revoke (may be null)
     */
    void logout(String refreshToken);

    /**
     * Revokes every refresh token belonging to the user (logout-all).
     *
     * @param userId the authenticated user's ID
     */
    void logoutAll(Long userId);

    /**
     * Sends a password-reset OTP to the given email.
     *
     * @param request the forgot-password payload (email)
     * @return an {@link OtpDispatchResponse} with OTP dispatch info
     */
    OtpDispatchResponse forgotPassword(ForgotPasswordRequest request);

    /**
     * Verifies the password-reset OTP and returns a short-lived reset token.
     *
     * @param challengeToken the challenge UUID from the forgot-password response
     * @param code           the OTP code entered by the user
     * @return a {@link ResetTokenResponse} containing the short-lived reset JWT
     */
    ResetTokenResponse verifyForgotPassword(String challengeToken, String code);

    /**
     * Sets a new password using a verified reset token.
     *
     * @param request the reset payload (resetToken + newPassword)
     */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Changes the authenticated user's password after verifying the current one.
     *
     * @param userId          the authenticated user's ID
     * @param currentPassword the user's current password
     * @param newPassword     the desired new password
     */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * Validates a JWT token and returns the associated user details.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return an {@link AuthResponse} containing the token and user details,
     *         or throws an exception if the token is invalid
     */
    AuthResponse validateToken(String token);
}
