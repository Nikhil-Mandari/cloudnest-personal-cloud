package com.cloudnest.auth.controller;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.PasskeyAuthenticationFinishRequest;
import com.cloudnest.auth.dto.PasskeyAuthenticationStartRequest;
import com.cloudnest.auth.dto.PasskeyAuthenticationStartResponse;
import com.cloudnest.auth.dto.PasskeyCredentialResponse;
import com.cloudnest.auth.dto.PasskeyRegistrationFinishRequest;
import com.cloudnest.auth.dto.PasskeyRegistrationStartResponse;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.security.ClientInfoFactory;
import com.cloudnest.auth.service.PasskeyCredentialService;
import com.cloudnest.auth.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
 * WebAuthn passkey endpoints.
 * <p>
 * Registration, listing and removal are authenticated (gateway-injected
 * {@code X-User-Id}). Sign-in start/finish are public — they are the login
 * flow for a user who has already registered a passkey.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/passkeys")
@Tag(name = "Passkeys", description = "WebAuthn passkey registration, management and biometric sign-in.")
public class PasskeyController {

    private static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final PasskeyCredentialService passkeyService;
    private final ClientInfoFactory clientInfoFactory;

    public PasskeyController(PasskeyCredentialService passkeyService, ClientInfoFactory clientInfoFactory) {
        this.passkeyService = passkeyService;
        this.clientInfoFactory = clientInfoFactory;
    }

    // ── Registration / management (authenticated) ────────────────────────────

    /**
     * Starts a passkey registration ceremony.
     */
    @Operation(summary = "Start passkey registration",
            description = "Returns WebAuthn creation options (discoverable credential) for the browser.")
    @PostMapping("/register/start")
    public ResponseEntity<StandardResponse<PasskeyRegistrationStartResponse>> registerStart(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/passkeys/register/start - userId={}", userIdHeader);

        PasskeyRegistrationStartResponse response = passkeyService.registerStart(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<PasskeyRegistrationStartResponse>builder()
                .success(true)
                .message("Authenticate to finish registering your passkey")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Finishes a passkey registration ceremony.
     */
    @Operation(summary = "Finish passkey registration",
            description = "Verifies the attestation response and stores the credential.")
    @PostMapping("/register/finish")
    public ResponseEntity<StandardResponse<PasskeyCredentialResponse>> registerFinish(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody PasskeyRegistrationFinishRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/passkeys/register/finish - userId={}", userIdHeader);

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        PasskeyCredentialResponse response =
                passkeyService.registerFinish(userIdHeader, request, clientInfo);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<PasskeyCredentialResponse>builder()
                        .success(true)
                        .message("Passkey registered — you can now sign in with it")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Lists the user's registered passkeys.
     */
    @Operation(summary = "List passkeys",
            description = "Returns every passkey registered to the account, newest first.")
    @GetMapping
    public ResponseEntity<StandardResponse<List<PasskeyCredentialResponse>>> list(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        List<PasskeyCredentialResponse> credentials = passkeyService.list(userIdHeader);

        return ResponseEntity.ok(StandardResponse.<List<PasskeyCredentialResponse>>builder()
                .success(true)
                .message("Passkeys retrieved successfully")
                .data(credentials)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Removes a registered passkey.
     */
    @Operation(summary = "Remove a passkey",
            description = "Deletes the passkey with the given internal id.")
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> remove(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/auth/passkeys/{} - userId={}", id, userIdHeader);

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        passkeyService.remove(userIdHeader, id, clientInfo);

        return ResponseEntity.ok(StandardResponse.<Void>builder()
                .success(true)
                .message("Passkey removed")
                .path(httpRequest.getRequestURI())
                .build());
    }

    // ── Sign-in (public) ─────────────────────────────────────────────────────

    /**
     * Starts a passkey sign-in ceremony (public).
     */
    @Operation(summary = "Start passkey sign-in",
            description = "Returns WebAuthn assertion options for the browser. Discovery-less: the "
                    + "browser offers every discoverable credential for this site.")
    @PostMapping("/authenticate/start")
    public ResponseEntity<StandardResponse<PasskeyAuthenticationStartResponse>> authenticateStart(
            @RequestBody(required = false) PasskeyAuthenticationStartRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/passkeys/authenticate/start");

        PasskeyAuthenticationStartResponse response = passkeyService.authenticateStart(
                request == null ? new PasskeyAuthenticationStartRequest() : request);

        return ResponseEntity.ok(StandardResponse.<PasskeyAuthenticationStartResponse>builder()
                .success(true)
                .message("Authenticate to sign in with your passkey")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }

    /**
     * Finishes a passkey sign-in ceremony and completes the login (public).
     */
    @Operation(summary = "Finish passkey sign-in",
            description = "Verifies the assertion and returns the JWT token pair. A valid passkey "
                    + "assertion satisfies both authentication factors.")
    @PostMapping("/authenticate/finish")
    public ResponseEntity<StandardResponse<AuthResponse>> authenticateFinish(
            @Valid @RequestBody PasskeyAuthenticationFinishRequest request,
            @RequestHeader(value = DEVICE_ID_HEADER, required = false) String deviceId,
            HttpServletRequest httpRequest) {

        log.info("POST /api/auth/passkeys/authenticate/finish");

        ClientInfo clientInfo = clientInfoFactory.from(httpRequest);
        AuthResponse response = passkeyService.authenticateFinish(request, clientInfo, deviceId);

        return ResponseEntity.ok(StandardResponse.<AuthResponse>builder()
                .success(true)
                .message("Passkey sign-in successful")
                .data(response)
                .path(httpRequest.getRequestURI())
                .build());
    }
}
