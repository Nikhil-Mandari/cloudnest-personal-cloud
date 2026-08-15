package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Per-user two-factor authentication (TOTP) settings.
 * <p>
 * The TOTP shared secret is stored base32-encoded. Enabling 2FA additionally
 * generates one-time backup codes (stored separately, hashed).
 */
@Entity
@Table(name = "two_factor_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner (matches {@code auth_db.user_credentials.id}). */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Base32 TOTP shared secret. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String secret;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
