package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.config.WebAuthnConfig;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.PasskeyAuthenticationFinish;
import com.cloudnest.auth.dto.PasskeyAuthenticationStart;
import com.cloudnest.auth.dto.PasskeyCredentialInfo;
import com.cloudnest.auth.dto.PasskeyRegistrationFinish;
import com.cloudnest.auth.dto.PasskeyRegistrationStart;
import com.cloudnest.auth.entity.PasskeyCredential;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.repository.PasskeyCredentialRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.PasskeyService;
import com.cloudnest.auth.service.SecurityService;
import com.cloudnest.auth.service.TokenIssuer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WebAuthn passkey registration and sign-in backed by the Yubico
 * {@link RelyingParty}. The browser-facing options are produced with the
 * library's {@code toCredentialsCreateJson()/toCredentialsGetJson()} helpers,
 * and the echoed {@code requestJson}/{@code optionsJson} is used to rebuild
 * the exact server-side request for verification.
 */
@Slf4j
@Service
public class PasskeyServiceImpl implements PasskeyService {

    private static final long CHALLENGE_TIMEOUT_MS = 120_000L;

    private final RelyingParty relyingParty;
    private final UserCredentialRepository userRepository;
    private final PasskeyCredentialRepository passkeyRepository;
    private final TokenIssuer tokenIssuer;
    private final SecurityService securityService;
    private final ObjectMapper objectMapper;

    public PasskeyServiceImpl(RelyingParty relyingParty,
                              UserCredentialRepository userRepository,
                              PasskeyCredentialRepository passkeyRepository,
                              TokenIssuer tokenIssuer,
                              SecurityService securityService,
                              ObjectMapper objectMapper) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
        this.passkeyRepository = passkeyRepository;
        this.tokenIssuer = tokenIssuer;
        this.securityService = securityService;
        this.objectMapper = objectMapper;
    }

    // ── Registration ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PasskeyRegistrationStart startRegistration(Long userId) {
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserIdentity identity = UserIdentity.builder()
                .name(user.getEmail())
                .displayName(user.getUsername())
                .id(WebAuthnConfig.userHandle(userId))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(identity)
                .timeout(CHALLENGE_TIMEOUT_MS)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        // Resident (discoverable) keys are REQUIRED because
                        // sign-in resolves the account from the assertion's
                        // user handle; non-discoverable credentials cannot.
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .build())
                .build();

        try {
            PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
            return PasskeyRegistrationStart.builder()
                    .optionsJson(creationOptions.toCredentialsCreateJson())
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize registration options", e);
        }
    }

    @Override
    @Transactional
    public PasskeyCredentialInfo finishRegistration(Long userId, PasskeyRegistrationFinish request) {
        try {
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential =
                    PublicKeyCredential.parseRegistrationResponseJson(request.getResponseJson());
            PublicKeyCredentialCreationOptions creationOptions =
                    PublicKeyCredentialCreationOptions.fromJson(request.getOptionsJson());

            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(creationOptions)
                    .response(credential)
                    .build());

            String credentialId = result.getKeyId().getId().getBase64Url();
            if (passkeyRepository.findByCredentialId(credentialId).isPresent()) {
                throw new DuplicateResourceException("This passkey is already registered");
            }

            List<String> transports = credential.getResponse().getTransports() == null
                    ? List.of()
                    : credential.getResponse().getTransports().stream()
                            .map(AuthenticatorTransport::getId)
                            .toList();

            PasskeyCredential saved = passkeyRepository.save(PasskeyCredential.builder()
                    .userId(userId)
                    .credentialId(credentialId)
                    .publicKeyCose(result.getPublicKeyCose().getBytes())
                    .signCount(result.getSignatureCount())
                    .nickname(sanitizeNickname(request.getNickname()))
                    .transports(toJson(transports))
                    .build());

            securityService.logEvent(userId, "PASSKEY_REGISTERED",
                    "Passkey registered" + (saved.getNickname() != null ? ": " + saved.getNickname() : ""));
            log.info("Passkey registered for userId={}, credentialId={}", userId, credentialId);
            return toInfo(saved);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid passkey registration response", e);
        } catch (com.yubico.webauthn.exception.RegistrationFailedException e) {
            log.warn("Passkey registration failed for userId={}: {}", userId, e.getMessage());
            throw new IllegalArgumentException("Passkey registration failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasskeyCredentialInfo> listPasskeys(Long userId) {
        return passkeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toInfo)
                .toList();
    }

    @Override
    @Transactional
    public void removePasskey(Long userId, String credentialId) {
        passkeyRepository.deleteByUserIdAndCredentialId(userId, credentialId);
        securityService.logEvent(userId, "PASSKEY_REMOVED", "Passkey removed");
        log.info("Passkey removed for userId={}, credentialId={}", userId, credentialId);
    }

    // ── Authentication ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PasskeyAuthenticationStart startAuthentication() {
        StartAssertionOptions options = StartAssertionOptions.builder()
                .timeout(CHALLENGE_TIMEOUT_MS)
                .build();
        AssertionRequest request = relyingParty.startAssertion(options);
        try {
            return PasskeyAuthenticationStart.builder()
                    .requestJson(request.toJson())
                    .credentialsGetJson(request.toCredentialsGetJson())
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize assertion request", e);
        }
    }

    @Override
    @Transactional
    public AuthResponse finishAuthentication(PasskeyAuthenticationFinish request, DeviceInfo device) {
        try {
            AssertionRequest assertionRequest = AssertionRequest.fromJson(request.getRequestJson());
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential =
                    PublicKeyCredential.parseAssertionResponseJson(request.getResponseJson());

            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(credential)
                    .build());

            Long userId = WebAuthnConfig.userIdFromHandle(result.getUserHandle())
                    .orElseThrow(() -> new IllegalArgumentException("Passkey is not linked to an account"));

            UserCredential user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found for passkey"));

            passkeyRepository.findByCredentialId(result.getCredentialId().getBase64Url())
                    .ifPresent(cred -> {
                        cred.setSignCount(result.getSignatureCount());
                        cred.setLastUsedAt(LocalDateTime.now());
                        passkeyRepository.save(cred);
                    });

            securityService.recordLoginSuccess(userId, device);
            securityService.logEvent(userId, "PASSKEY_VERIFIED", "Signed in with a passkey");
            log.info("Passkey sign-in for userId={}, credentialId={}", userId, result.getCredentialId().getBase64Url());

            return tokenIssuer.issue(user, device);
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Invalid passkey assertion response", e);
        } catch (com.yubico.webauthn.exception.AssertionFailedException e) {
            log.warn("Passkey assertion failed: {}", e.getMessage());
            throw new IllegalArgumentException("Passkey sign-in failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private PasskeyCredentialInfo toInfo(PasskeyCredential credential) {
        List<String> transports = fromJson(credential.getTransports());
        return PasskeyCredentialInfo.builder()
                .id(credential.getCredentialId())
                .nickname(credential.getNickname())
                .transports(transports)
                .createdAt(credential.getCreatedAt())
                .lastUsedAt(credential.getLastUsedAt())
                .build();
    }

    private String sanitizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }
        return nickname.trim().length() > 80 ? nickname.trim().substring(0, 80) : nickname.trim();
    }

    private String toJson(List<String> transports) {
        try {
            return objectMapper.writeValueAsString(transports);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
