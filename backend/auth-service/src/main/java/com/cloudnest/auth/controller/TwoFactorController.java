package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.DisableTwoFactorRequest;
import com.cloudnest.auth.dto.EnableTwoFactorRequest;
import com.cloudnest.auth.dto.EnableTwoFactorResponse;
import com.cloudnest.auth.dto.RegenerateBackupCodesResponse;
import com.cloudnest.auth.dto.TwoFactorSetupResponse;
import com.cloudnest.auth.dto.TwoFactorStatusResponse;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.security.ClientInfoFactory;
import com.cloudnest.auth.service.TwoFactorService;
import com.cloudnest.auth.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * TOTP two-factor-authentication management (authenticated).
 * <p>
 * All endpoints require a valid access token (gateway-injected
 * {@code X-User-Id} header) — the same trust model as the other protected
 * auth endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
@Tag(name = "Two-Factor Authentication", description = "TOTP 2FA setup, enable/disable, backup codes and status.")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final ClientInfoFactory clientInfoFactory;

    public TwoFactorController(TwoFactorService twoFactorService, ClientInfoFactory clientInfoFactory) {
        this.twoFactorService = twoFactorService;
        this.clientInfoFactory = clientInfoFactory;
    }

    /**
     * Current 2FA state (enabled flag + unused backup codes).
     */
    @Operation(summary = "2FA status",
            description = "Whether TOTP 2FA is enabled and how many unused backup codes remain.")
    @GetMapping("/status")
    public ResponseEntity<StandardResponse<TwoFactorStatusResponse>> status(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        TwoFactorStatusResponse status = twoFactorService.status(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<TwoFactorStatusResponse>builder()
                .success(true)
                .message("Two-factor status retrieved")
                .data(status)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Starts a 2FA setup: returns the secret + otpauth URI (QR payload).
     */
    @Operation(summary = "Start 2FA setup",
            description = "Generates (or re-returns) the TOTP secret and otpauth:// URI the authenticator "
                    + "app scans. The user must submit a live code to enable.")
    @PostMapping("/setup")
    public ResponseEntity<StandardResponse<TwoFactorSetupResponse>> setup(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        TwoFactorSetupResponse setup = twoFactorService.setup(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<TwoFactorSetupResponse>builder()
                .success(true)
                .message("Scan the QR code with your authenticator app")
                .data(setup)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Enables 2FA after verifying a live authenticator code; returns the
     * one-time backup codes.
     */
    @Operation(summary = "Enable 2FA",
            description = "Verifies the authenticator code, enables TOTP 2FA and returns the freshly "
                    + "generated backup codes (shown exactly once).")
    @PostMapping("/enable")
    public ResponseEntity<StandardResponse<EnableTwoFactorResponse>> enable(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody EnableTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        EnableTwoFactorResponse response =
                twoFactorService.enable(userIdHeader, request.getCode(), clientInfo);

        return ResponseEntity.ok(StandardResponse.<EnableTwoFactorResponse>builder()
                .success(true)
                .message("Two-factor authentication enabled — save your backup codes")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Disables 2FA after verifying the caller controls the account.
     */
    @Operation(summary = "Disable 2FA",
            description = "Removes TOTP 2FA after verification (authenticator code, unused backup code "
                    + "or account password). Backup codes are invalidated.")
    @PostMapping("/disable")
    public ResponseEntity<StandardResponse<Void>> disable(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody DisableTwoFactorRequest request,
            HttpServletRequest httpRequest) {

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        twoFactorService.disable(userIdHeader, request.getVerification(), clientInfo);

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Two-factor authentication disabled")
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Regenerates the backup-code set (old unused codes are invalidated).
     */
    @Operation(summary = "Regenerate backup codes",
            description = "Invalidates all existing backup codes and issues a fresh set (shown exactly once).")
    @PostMapping("/backup-codes/regenerate")
    public ResponseEntity<StandardResponse<RegenerateBackupCodesResponse>> regenerateBackupCodes(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        RegenerateBackupCodesResponse response =
                twoFactorService.regenerateBackupCodes(userIdHeader, clientInfo);

        return ResponseEntity.ok(StandardResponse.<RegenerateBackupCodesResponse>builder()
                .success(true)
                .message("Backup codes regenerated — store the new codes")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }
}
