package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.ForgotPasswordRequest;
import com.cloudnest.auth.dto.LoginRequest;
import com.cloudnest.auth.dto.LoginResponse;
import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.dto.RegisterRequest;
import com.cloudnest.auth.dto.RegisterResponse;
import com.cloudnest.auth.dto.ResetPasswordRequest;
import com.cloudnest.auth.dto.ResetTokenResponse;

/**
 * Authentication operations.
 */
public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    AuthResponse verifyRegistration(String email, String code, boolean rememberDevice, DeviceInfo device);

    OtpDispatchResponse resendOtp(String email, String challengeToken);

    LoginResponse login(LoginRequest request, DeviceInfo device);

    AuthResponse verifyLogin(String challengeToken, String code, boolean rememberDevice, DeviceInfo device);

    /**
     * Completes a login blocked on the 2FA step with a TOTP or backup code.
     *
     * @param challengeToken short-lived JWT issued by {@link #login}
     * @param code           TOTP code or unused backup code
     */
    AuthResponse verifyTwoFactorLogin(String challengeToken, String code,
                                      boolean rememberDevice, DeviceInfo device);

    AuthResponse refreshToken(String refreshToken, DeviceInfo device);

    void logout(String refreshToken, DeviceInfo device);

    void logoutAll(Long userId, DeviceInfo device);

    OtpDispatchResponse forgotPassword(ForgotPasswordRequest request);

    ResetTokenResponse verifyForgotPassword(String challengeToken, String code);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Long userId, String currentPassword, String newPassword);

    AuthResponse validateToken(String token);
}
