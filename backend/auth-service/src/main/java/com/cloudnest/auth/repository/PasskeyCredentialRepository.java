package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.PasskeyCredential;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Repository for {@link PasskeyCredential} that doubles as the WebAuthn
 * {@link CredentialRepository} backing the relying party.
 * <p>
 * The user handle stored by the WebAuthn library is the UTF-8 encoding of the
 * CloudNest user id — that keeps {@code getUserHandleForUsername} /
 * {@code getUsernameForUserHandle} trivial and unambiguous.
 */
@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredential, String>,
        CredentialRepository {

    List<PasskeyCredential> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<PasskeyCredential> findByCredentialId(String credentialId);

    // ── WebAuthn CredentialRepository contract ──────────────────────────────

    @Override
    default Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // Discovery-less assertions: the browser offers resident credentials
        // directly, so no username-scoped descriptor lookup is needed.
        return Set.of();
    }

    @Override
    default Optional<ByteArray> getUserHandleForUsername(String username) {
        // Usernames are resolved by the AuthService (which owns credentials);
        // the relying party only ever starts discovery-less assertions.
        return Optional.empty();
    }

    @Override
    default Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return Optional.of(new String(userHandle.getBytes(), StandardCharsets.UTF_8));
    }

    @Override
    default Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return findByCredentialId(credentialId.getBase64Url())
                .filter(cred -> new String(userHandle.getBytes(), StandardCharsets.UTF_8)
                        .equals(String.valueOf(cred.getUserId())))
                .map(PasskeyCredentialRepository::toRegisteredCredential);
    }

    @Override
    default Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        return findByCredentialId(credentialId.getBase64Url())
                .stream()
                .map(PasskeyCredentialRepository::toRegisteredCredential)
                .collect(Collectors.toSet());
    }

    static RegisteredCredential toRegisteredCredential(PasskeyCredential entity) {
        try {
            return RegisteredCredential.builder()
                    .credentialId(ByteArray.fromBase64Url(entity.getCredentialId()))
                    .userHandle(ByteArray.fromBase64Url(entity.getUserHandle()))
                    .publicKeyCose(ByteArray.fromBase64Url(entity.getPublicKeyCose()))
                    .signatureCount(entity.getSignatureCount())
                    .build();
        } catch (com.yubico.webauthn.data.exception.Base64UrlException e) {
            // Stored values were produced by this service and are always valid.
            throw new IllegalStateException("Stored passkey credential data is corrupt", e);
        }
    }
}
