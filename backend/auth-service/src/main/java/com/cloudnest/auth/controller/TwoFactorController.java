package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.DisableTwoFactorRequest;
import com.cloudnest.auth.dto.EnableTwoFactorRequest;
import com.cloudnest.auth.dto.EnableTwoFactorResponse;
import com.cloudnest.auth.dto.RegenerateBackupCodesResponse;
import com.cloudnest.auth.dto.TwoFactorSetup;
import com.cloudnest.auth.dto.TwoFactorStatus;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.TwoFactorService;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Two-factor authentication (TOTP) management endpoints.
 * <p>
 * All endpoints operate on the authenticated user (the gateway forwards
 * {@code X-User-Id}).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserCredentialRepository userRepository;

    public TwoFactorController(TwoFactorService twoFactorService, UserCredentialRepository userRepository) {
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<StandardResponse<TwoFactorStatus>> status(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        TwoFactorStatus status = twoFactorService.getStatus(userId);
        return ResponseEntity.ok(StandardResponse.<TwoFactorStatus>builder()
                .success(true)
                .message("Two-factor status retrieved")
                .data(status)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/setup")
    public ResponseEntity<StandardResponse<TwoFactorSetup>> setup(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        String email = userRepository.findById(userId)
                .map(user -> user.getEmail())
                .orElse("user-" + userId);
        TwoFactorSetup setup = twoFactorService.startSetup(userId, email);
        return ResponseEntity.ok(StandardResponse.<TwoFactorSetup>builder()
                .success(true)
                .message("Scan the QR code with your authenticator app")
                .data(setup)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/enable")
    public ResponseEntity<StandardResponse<EnableTwoFactorResponse>> enable(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody EnableTwoFactorRequest request,
            HttpServletRequest httpRequest) {
        EnableTwoFactorResponse response = twoFactorService.enable(userId, request.getCode());
        return ResponseEntity.ok(StandardResponse.<EnableTwoFactorResponse>builder()
                .success(true)
                .message("Two-factor authentication enabled")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/disable")
    public ResponseEntity<StandardResponse<Void>> disable(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DisableTwoFactorRequest request,
            HttpServletRequest httpRequest) {
        String password = userRepository.findById(userId)
                .map(user -> user.getPassword())
                .orElse(null);
        twoFactorService.disable(userId, request.getVerification(), password);
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Two-factor authentication disabled")
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<StandardResponse<RegenerateBackupCodesResponse>> regenerate(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        RegenerateBackupCodesResponse response = RegenerateBackupCodesResponse.builder()
                .backupCodes(twoFactorService.regenerateBackupCodes(userId))
                .build();
        return ResponseEntity.ok(StandardResponse.<RegenerateBackupCodesResponse>builder()
                .success(true)
                .message("New backup codes generated")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }
}
