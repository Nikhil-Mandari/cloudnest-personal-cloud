package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a one-time passcode issued to a user for a specific purpose.
 * <p>
 * The raw code is <em>never</em> stored — only its SHA-256 hash. Every
 * record carries its purpose ({@code REGISTRATION}, {@code LOGIN},
 * {@code PASSWORD_RESET}), an expiry timestamp, and the remaining
 * verification attempts so brute-forcing is bounded.
 */
@Entity
@Table(name = "otp_verifications", indexes = {
        @Index(name = "idx_otp_user_purpose", columnList = "user_id, purpose")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    /** The purpose this OTP was issued for. */
    public enum Purpose {
        REGISTRATION,
        LOGIN,
        PASSWORD_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private Purpose purpose;

    /** SHA-256 hash of the raw code (the code itself is never persisted). */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** How many times the code has been submitted (bounded by maxAttempts). */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "consumed", nullable = false)
    @Builder.Default
    private Boolean consumed = false;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    /** When the code was most recently resent (drives the cooldown timer). */
    @Column(name = "resent_at")
    private LocalDateTime resentAt;
}
