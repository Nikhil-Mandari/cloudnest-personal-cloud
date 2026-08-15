package com.cloudnest.auth.config;

import com.cloudnest.auth.repository.PasskeyCredentialRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebAuthn (passkey) configuration.
 * <p>
 * Builds the {@link RelyingParty} with a JPA-backed {@link CredentialRepository}
 * and the allowed origins from configuration. The {@code rp-id} must be the
 * effective domain the browser sees (e.g. {@code localhost} in development);
 * {@code allowOriginPort(true)} lets local origins carry a port.
 */
@Slf4j
@Configuration
public class WebAuthnConfig {

    @Bean
    public CredentialRepository webAuthnCredentialRepository(
            PasskeyCredentialRepository passkeyRepository,
            UserCredentialRepository userRepository) {
        return new CredentialRepository() {

            @Override
            public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
                return userRepository.findByUsername(username)
                        .map(user -> passkeyRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                                .map(credential -> PublicKeyCredentialDescriptor.builder()
                                        .id(decodeBase64Url(credential.getCredentialId()))
                                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                                        .build())
                                .collect(Collectors.toSet()))
                        .orElseGet(Set::of);
            }

            @Override
            public Optional<ByteArray> getUserHandleForUsername(String username) {
                return userRepository.findByUsername(username)
                        .map(user -> userHandle(user.getId()));
            }

            @Override
            public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
                return userIdFromHandle(userHandle)
                        .flatMap(userRepository::findById)
                        .map(user -> user.getUsername());
            }

            @Override
            public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
                return passkeyRepository.findByCredentialId(credentialId.getBase64Url())
                        .map(credential -> RegisteredCredential.builder()
                                .credentialId(credentialId)
                                .userHandle(userHandle)
                                .publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
                                .signatureCount(credential.getSignCount())
                                .build());
            }

            @Override
            public Set<RegisteredCredential> lookupAll(ByteArray userHandle) {
                return userIdFromHandle(userHandle)
                        .map(userId -> passkeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                                .map(credential -> RegisteredCredential.builder()
                                        .credentialId(decodeBase64Url(credential.getCredentialId()))
                                        .userHandle(userHandle)
                                        .publicKeyCose(new ByteArray(credential.getPublicKeyCose()))
                                        .signatureCount(credential.getSignCount())
                                        .build())
                                .collect(Collectors.toSet()))
                        .orElseGet(Set::of);
            }
        };
    }

    @Bean
    public RelyingParty relyingParty(CredentialRepository webAuthnCredentialRepository,
                                     @Value("${webauthn.rp-id:localhost}") String rpId,
                                     @Value("${webauthn.rp-name:CloudNest}") String rpName,
                                     @Value("${webauthn.origins:http://localhost:5173,http://localhost:8080}") List<String> origins) {
        RelyingParty rp = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(rpId)
                        .name(rpName)
                        .build())
                .credentialRepository(webAuthnCredentialRepository)
                .origins(Set.copyOf(origins))
                .allowOriginPort(true)
                .build();
        log.info("WebAuthn RelyingParty configured: rpId={}, rpName={}, origins={}", rpId, rpName, origins);
        return rp;
    }

    /** Encodes a user id as an 8-byte big-endian user handle. */
    public static ByteArray userHandle(Long userId) {
        return new ByteArray(ByteBuffer.allocate(Long.BYTES).putLong(userId).array());
    }

    /** Decodes an 8-byte user handle back to a user id. */
    public static Optional<Long> userIdFromHandle(ByteArray userHandle) {
        byte[] bytes = userHandle.getBytes();
        if (bytes.length != Long.BYTES) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.wrap(bytes).getLong());
    }

    /** Decodes a stored base64url credential id (checked exception is impossible for stored values). */
    private static ByteArray decodeBase64Url(String value) {
        try {
            return ByteArray.fromBase64Url(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Stored credential id is not valid base64url", e);
        }
    }
}
