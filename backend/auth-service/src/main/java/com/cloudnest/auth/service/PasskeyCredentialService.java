package com.cloudnest.auth.service;

import com.cloudnest.auth.client.NotificationServiceClient;
import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.PasskeyAuthenticationFinishRequest;
import com.cloudnest.auth.dto.PasskeyAuthenticationStartRequest;
import com.cloudnest.auth.dto.PasskeyAuthenticationStartResponse;
import com.cloudnest.auth.dto.PasskeyCredentialResponse;
import com.cloudnest.auth.dto.PasskeyRegistrationFinishRequest;
import com.cloudnest.auth.dto.PasskeyRegistrationStartResponse;
import com.cloudnest.auth.entity.PasskeyCredential;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.repository.PasskeyCredentialRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.security.ClientInfo;
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
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WebAuthn (passkey) registration and sign-in orchestration.
 * <p>
 * Uses <b>discoverable (resident) credentials</b> so sign-in works with a
 * single tap — Face ID, Touch ID, Windows Hello or a security key — without
 * typing a username. The user handle is the UTF-8 encoding of the CloudNest
 * user id; the stateless protocol echoes the serialized request/options JSON
 * back from the browser on finish, so no server-side ceremony state is kept.
 * <p>
 * A successful passkey assertion satisfies both factors (possession of the
 * authenticator + user verification), so passkey sign-ins skip the emailed
 * OTP and TOTP steps.
 */
@Slf4j
@Service
public class PasskeyCredentialService {

    private final RelyingParty relyingParty;
    private final PasskeyCredentialRepository credentialRepository;
    private final UserCredentialRepository userRepository;
    private final SecurityEventService securityEvents;
    private final AuthService authService;

    public PasskeyCredentialService(RelyingParty relyingParty,
                                    PasskeyCredentialRepository credentialRepository,
                                    UserCredentialRepository userRepository,
                                    SecurityEventService securityEvents,
                                    AuthService authService) {
        this.relyingParty = relyingParty;
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.securityEvents = securityEvents;
        this.authService = authService;
    }

    // ── Registration ─────────────────────────────────────────────────────────

    /**
     * Begins a registration ceremony: builds discoverable-credential creation
     * options for the browser.
     */
    @Transactional(readOnly = true)
    public PasskeyRegistrationStartResponse registerStart(Long userId) {
        UserCredential user = requireUser(userId);

        UserIdentity identity = UserIdentity.builder()
                .name(user.getUsername())
                .displayName(user.getUsername())
                .id(new ByteArray(userId.toString().getBytes(StandardCharsets.UTF_8)))
                .build();

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(identity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .build())
                .build();

        PublicKeyCredentialCreationOptions request = relyingParty.startRegistration(options);
        try {
            return PasskeyRegistrationStartResponse.builder()
                    .optionsJson(request.toCredentialsCreateJson())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize WebAuthn creation options", e);
        }
    }

    /**
     * Finishes a registration ceremony, verifies the attestation and stores
     * the credential.
     */
    @Transactional
    public PasskeyCredentialResponse registerFinish(Long userId,
                                                    PasskeyRegistrationFinishRequest request,
                                                    ClientInfo clientInfo) {
        UserCredential user = requireUser(userId);

        try {
            PublicKeyCredentialCreationOptions options =
                    PublicKeyCredentialCreationOptions.fromJson(request.getOptionsJson());
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> response =
                    PublicKeyCredential.parseRegistrationResponseJson(request.getResponseJson());

            RegistrationResult result = relyingParty.finishRegistration(FinishRegistrationOptions.builder()
                    .request(options)
                    .response(response)
                    .build());

            String credentialId = result.getKeyId().getId().getBase64Url();
            if (credentialRepository.findByCredentialId(credentialId).isPresent()) {
                throw new DuplicateResourceException("This passkey is already registered to your account");
            }

            PasskeyCredential entity = PasskeyCredential.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .credentialId(credentialId)
                    .userHandle(new ByteArray(userId.toString().getBytes(StandardCharsets.UTF_8)).getBase64Url())
                    .publicKeyCose(result.getPublicKeyCose().getBase64Url())
                    .signatureCount(result.getSignatureCount())
                    .transports(joinTransports(
                            result.getKeyId().getTransports().orElse(java.util.Collections.emptySortedSet())))
                    .nickname(trimToNull(request.getNickname()))
                    .build();
            entity = credentialRepository.save(entity);

            securityEvents.log(user, SecurityEventService.ACTION_PASSKEY_REGISTERED, clientInfo,
                    "Passkey registered: " + label(entity));
            String client = securityEvents.describeClient(clientInfo);
            securityEvents.notify(user, NotificationServiceClient.TYPE_PASSKEY_REGISTERED,
                    "New passkey added",
                    (client == null ? "A passkey was registered" : "A passkey was registered from " + client)
                            + " for your account"
                            + (entity.getNickname() == null ? "." : " (" + entity.getNickname() + ").")
                            + " If this wasn't you, remove it from Security settings.");

            log.info("Passkey registered for userId={}, credential={}", userId, entity.getNickname());
            return toResponse(entity);
        } catch (RegistrationFailedException e) {
            throw new IllegalArgumentException("Passkey registration was rejected: " + e.getMessage());
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid passkey registration payload");
        }
    }

    // ── Listing / removal ────────────────────────────────────────────────────

    /**
     * Lists the user's registered passkeys, newest first.
     */
    @Transactional(readOnly = true)
    public List<PasskeyCredentialResponse> list(Long userId) {
        return credentialRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Removes a passkey (by its internal id) belonging to the user.
     */
    @Transactional
    public void remove(Long userId, String id, ClientInfo clientInfo) {
        PasskeyCredential credential = credentialRepository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Passkey not found"));

        credentialRepository.delete(credential);

        UserCredential user = requireUser(userId);
        securityEvents.log(user, SecurityEventService.ACTION_PASSKEY_REMOVED, clientInfo,
                "Passkey removed: " + label(credential));
        securityEvents.notify(user, NotificationServiceClient.TYPE_PASSKEY_REMOVED,
                "Passkey removed",
                "The passkey"
                        + (credential.getNickname() == null ? "" : " '" + credential.getNickname() + "'")
                        + " was removed from your account."
                        + " If this wasn't you, review your security settings immediately.");

        log.info("Passkey removed for userId={}, id={}", userId, id);
    }

    // ── Sign-in (assertion) ──────────────────────────────────────────────────

    /**
     * Begins a passkey sign-in. Discovery-less (no username needed): the
     * browser offers every discoverable credential for this relying party.
     */
    @Transactional(readOnly = true)
    public PasskeyAuthenticationStartResponse authenticateStart(PasskeyAuthenticationStartRequest request) {
        StartAssertionOptions options = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();

        AssertionRequest assertionRequest = relyingParty.startAssertion(options);
        try {
            return PasskeyAuthenticationStartResponse.builder()
                    .requestJson(assertionRequest.toJson())
                    .credentialsGetJson(assertionRequest.toCredentialsGetJson())
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize WebAuthn assertion request", e);
        }
    }

    /**
     * Finishes a passkey sign-in: verifies the assertion, refreshes the
     * signature counter, and completes the login.
     */
    @Transactional
    public AuthResponse authenticateFinish(PasskeyAuthenticationFinishRequest request,
                                           ClientInfo clientInfo, String deviceId) {
        try {
            AssertionRequest assertionRequest = AssertionRequest.fromJson(request.getRequestJson());
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> response =
                    PublicKeyCredential.parseAssertionResponseJson(request.getResponseJson());

            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(response)
                    .build());

            if (!result.isSuccess()) {
                throw new BadCredentialsException("Passkey verification failed");
            }

            // The relying party already rejects signature-counter regression
            // (cloned-authenticator detection); persist the fresh counter.
            Long userId = Long.valueOf(result.getUsername());
            credentialRepository.findByCredentialId(result.getCredentialId().getBase64Url())
                    .ifPresent(credential -> {
                        credential.setSignatureCount(result.getSignatureCount());
                        credential.setLastUsedAt(LocalDateTime.now());
                        credentialRepository.save(credential);
                    });

            log.info("Passkey assertion succeeded for userId={}", userId);
            return authService.completePasskeyLogin(userId, clientInfo, deviceId);
        } catch (AssertionFailedException e) {
            log.warn("Passkey assertion rejected: {}", e.getMessage());
            throw new BadCredentialsException("Passkey verification failed");
        } catch (IOException e) {
            throw new BadCredentialsException("Invalid passkey sign-in payload");
        } catch (NumberFormatException e) {
            throw new BadCredentialsException("Passkey verification failed");
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private UserCredential requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private PasskeyCredentialResponse toResponse(PasskeyCredential entity) {
        return PasskeyCredentialResponse.builder()
                .id(entity.getId())
                .nickname(entity.getNickname())
                .transports(splitTransports(entity.getTransports()))
                .createdAt(entity.getCreatedAt())
                .lastUsedAt(entity.getLastUsedAt())
                .build();
    }

    private String joinTransports(java.util.SortedSet<AuthenticatorTransport> transports) {
        if (transports == null || transports.isEmpty()) {
            return null;
        }
        return transports.stream()
                .map(AuthenticatorTransport::getId)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private List<String> splitTransports(String transports) {
        if (transports == null || transports.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String label(PasskeyCredential credential) {
        return credential.getNickname() == null ? "unlabelled" : credential.getNickname();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
