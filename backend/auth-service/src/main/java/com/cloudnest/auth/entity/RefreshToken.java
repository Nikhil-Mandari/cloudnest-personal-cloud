package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persistent, opaque refresh token used by the Axios 401-recovery interceptor
 * to rotate access tokens without forcing a re-login.
 * <p>
 * Only the SHA-256 hash of the token is stored (never the raw value).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner of the token (matches {@code auth_db.user_credentials.id}). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** SHA-256 hash of the raw refresh token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    // ── Device / session metadata (Security page sessions list) ───────────

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(length = 60)
    private String browser;

    @Column(length = 60)
    private String os;

    @Column(name = "device_type", length = 16)
    private String deviceType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 80)
    private String location;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}