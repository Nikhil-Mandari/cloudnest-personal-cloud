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
 * An active sign-in session (one per device) for the authenticated user.
 * <p>
 * Sessions power the "Active devices / sessions" screen and enable
 * "log out this device" and "log out all devices" — ending a session also
 * revokes every refresh token bound to it.
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_session_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Public session identifier (UUID) embedded in access/refresh tokens. */
    @Column(name = "session_id", nullable = false, unique = true, length = 36)
    private String sessionId;

    /** Stable client-generated device identifier. */
    @Column(name = "device_id", nullable = false, length = 64)
    private String deviceId;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "is_trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = false;

    @Column(name = "login_time", nullable = false, updatable = false)
    private LocalDateTime loginTime;

    @Column(name = "last_active", nullable = false)
    private LocalDateTime lastActive;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
