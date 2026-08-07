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
 * Append-only record of every sign-in attempt (successful or failed).
 * <p>
 * Powers the "Login history" screen and feeds the unknown-device detection
 * logic: a successful login is compared against prior history and trusted
 * devices before an alert email is sent.
 */
@Entity
@Table(name = "login_history", indexes = {
        @Index(name = "idx_login_user_time", columnList = "user_id, login_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "os", length = 100)
    private String os;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "login_time", nullable = false, updatable = false)
    private LocalDateTime loginTime;
}
