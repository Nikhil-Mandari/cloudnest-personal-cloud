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
 * Stores time-limited, securely hashed OTPs for email verification flows
 * (registration, login verification, password reset).
 */
@Entity
@Table(name = "otp_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email address this OTP was issued to. */
    @Column(nullable = false, length = 100)
    private String email;

    /** SHA-256 hash of the OTP code (never stored in plain text). */
    @Column(nullable = false, length = 64)
    private String otpHash;

    /** OTP purpose: REGISTRATION, LOGIN, PASSWORD_RESET. */
    @Column(nullable = false, length = 30)
    private String purpose;

    /**
     * Opaque challenge token (UUID) used in the login / password-reset flows
     * to bind the OTP to a specific pending challenge. Null for registration.
     */
    @Column(length = 36)
    private String challengeToken;

    /** Timestamp after which this OTP is no longer valid. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** How many failed attempts have been made against this record. */
    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    /** Whether this OTP has been successfully verified. */
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    /** Whether this record has been invalidated (by resend or expiry sweep). */
    @Column(nullable = false)
    @Builder.Default
    private boolean invalidated = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusMinutes(5);
        }
    }
}