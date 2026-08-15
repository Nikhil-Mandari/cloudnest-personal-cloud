package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A registered WebAuthn (passkey) credential owned by a user.
 * <p>
 * {@code credentialId} is stored as the base64url credential ID and the COSE
 * public key as raw bytes — exactly what the WebAuthn Relying Party needs to
 * verify future assertions.
 */
@Entity
@Table(name = "passkey_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Base64url credential ID (unique per credential). */
    @Column(name = "credential_id", nullable = false, unique = true, length = 512)
    private String credentialId;

    /** Raw COSE-encoded public key. */
    @Lob
    @Column(name = "public_key_cose", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] publicKeyCose;

    /** Signature counter used for cloned-authenticator detection. */
    @Column(name = "sign_count", nullable = false)
    @Builder.Default
    private long signCount = 0;

    @Column(length = 80)
    private String nickname;

    /** JSON array of {@code AuthenticatorTransport} identifiers. */
    @Column(columnDefinition = "TEXT")
    private String transports;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
