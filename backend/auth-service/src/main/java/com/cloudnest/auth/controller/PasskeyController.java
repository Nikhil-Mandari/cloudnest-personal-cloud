package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.PasskeyAuthenticationFinish;
import com.cloudnest.auth.dto.PasskeyAuthenticationStart;
import com.cloudnest.auth.dto.PasskeyCredentialInfo;
import com.cloudnest.auth.dto.PasskeyRegistrationFinish;
import com.cloudnest.auth.dto.PasskeyRegistrationStart;
import com.cloudnest.auth.service.PasskeyService;
import com.cloudnest.auth.util.DeviceInfoParser;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * WebAuthn passkey registration and authentication endpoints.
 * <p>
 * Registration + management require an authenticated user (gateway forwards
 * {@code X-User-Id}); the sign-in ceremony ({@code authenticate/start|finish})
 * is public so a logged-out user can authenticate with a passkey.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/passkeys")
public class PasskeyController {

    private final PasskeyService passkeyService;
    private final DeviceInfoParser deviceInfoParser;

    public PasskeyController(PasskeyService passkeyService, DeviceInfoParser deviceInfoParser) {
        this.passkeyService = passkeyService;
        this.deviceInfoParser = deviceInfoParser;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<List<PasskeyCredentialInfo>>> list(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        List<PasskeyCredentialInfo> passkeys = passkeyService.listPasskeys(userId);
        return ResponseEntity.ok(StandardResponse.<List<PasskeyCredentialInfo>>builder()
                .success(true)
                .message("Passkeys retrieved")
                .data(passkeys)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/register/start")
    public ResponseEntity<StandardResponse<PasskeyRegistrationStart>> registerStart(
            @RequestHeader("X-User-Id") Long userId,
            HttpServletRequest httpRequest) {
        PasskeyRegistrationStart response = passkeyService.startRegistration(userId);
        return ResponseEntity.ok(StandardResponse.<PasskeyRegistrationStart>builder()
                .success(true)
                .message("Registration options generated")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/register/finish")
    public ResponseEntity<StandardResponse<PasskeyCredentialInfo>> registerFinish(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PasskeyRegistrationFinish request,
            HttpServletRequest httpRequest) {
        PasskeyCredentialInfo credential = passkeyService.finishRegistration(userId, request);
        return ResponseEntity.ok(StandardResponse.<PasskeyCredentialInfo>builder()
                .success(true)
                .message("Passkey registered")
                .data(credential)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> remove(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") String id,
            HttpServletRequest httpRequest) {
        passkeyService.removePasskey(userId, id);
        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Passkey removed")
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/authenticate/start")
    public ResponseEntity<StandardResponse<PasskeyAuthenticationStart>> authenticateStart(
            HttpServletRequest httpRequest) {
        PasskeyAuthenticationStart response = passkeyService.startAuthentication();
        return ResponseEntity.ok(StandardResponse.<PasskeyAuthenticationStart>builder()
                .success(true)
                .message("Authentication options generated")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    @PostMapping("/authenticate/finish")
    public ResponseEntity<StandardResponse<AuthResponse>> authenticateFinish(
            @Valid @RequestBody PasskeyAuthenticationFinish request,
            HttpServletRequest httpRequest) {
        DeviceInfo device = deviceInfoParser.parse(httpRequest);
        AuthResponse authResponse = passkeyService.finishAuthentication(request, device);
        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Passkey sign-in successful")
                .data(authResponse)
                .path(httpRequest.getRequestURI())
                .build());
    }
}
