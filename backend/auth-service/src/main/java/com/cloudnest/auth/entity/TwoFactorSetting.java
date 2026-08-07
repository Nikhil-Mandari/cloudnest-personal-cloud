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
 * TOTP two-factor-authentication state for a user.
 * <p>
 * The secret is generated at setup time and stored (base32, no padding). The
 * row exists even before 2FA is enabled — {@code enabled} only flips after the
 * user proves they scanned the secret by submitting a valid code. Backup
 * codes live in {@code backup_codes}.
 */
@Entity
@Table(name = "two_factor_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Base32 TOTP secret (no padding). */
    @Column(nullable = false, length = 64)
    private String secret;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    /**
     * Last accepted TOTP time-step counter. Codes from an earlier (or equal)
     * step are rejected, so a captured code cannot be replayed within its
     * validity window.
     */
    @Column(name = "last_used_counter")
    private Long lastUsedCounter;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
