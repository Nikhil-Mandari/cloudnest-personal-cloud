package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A WebAuthn (passkey) credential registered to a user.
 * <p>
 * Stores the COSE public key and metadata the WebAuthn relying party needs to
 * verify future assertions. Binary values are stored base64url-encoded; the
 * signature counter is updated after every successful assertion to detect
 * cloned authenticators.
 */
@Entity
@Table(name = "passkey_credentials", indexes = @Index(name = "idx_passkeys_user", columnList = "user_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredential {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Base64url credential id (unique). */
    @Column(name = "credential_id", nullable = false, length = 512, unique = true)
    private String credentialId;

    /** Base64url user handle (encodes the CloudNest user id). */
    @Column(name = "user_handle", nullable = false, length = 255)
    private String userHandle;

    /** Base64url COSE public key. */
    @Column(name = "public_key_cose", nullable = false, length = 2048)
    private String publicKeyCose;

    @Column(name = "signature_count", nullable = false)
    @Builder.Default
    private Long signatureCount = 0L;

    /** Comma-separated {@code AuthenticatorTransport} names (may be null). */
    @Column(length = 255)
    private String transports;

    /** User-assigned label, e.g. "MacBook Touch ID". */
    @Column(length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
