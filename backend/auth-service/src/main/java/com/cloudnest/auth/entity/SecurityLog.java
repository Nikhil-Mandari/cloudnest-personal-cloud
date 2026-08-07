package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Append-only audit trail of security-relevant actions.
 * <p>
 * Actions include {@code LOGIN_SUCCESS}, {@code LOGIN_FAILED},
 * {@code LOGIN_LOCKED}, {@code PASSWORD_CHANGED}, {@code PASSWORD_RESET},
 * {@code LOGOUT}, {@code LOGOUT_ALL}, {@code OTP_VERIFIED},
 * {@code DEVICE_TRUSTED}, {@code DEVICE_UNTRUSTED}, {@code REFRESH_TOKEN},
 * {@code SESSION_ENDED}.
 */
@Entity
@Table(name = "security_logs", indexes = {
        @Index(name = "idx_security_user_time", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
