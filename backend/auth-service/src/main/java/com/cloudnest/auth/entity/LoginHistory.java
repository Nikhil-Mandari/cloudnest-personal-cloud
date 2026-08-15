package com.cloudnest.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * A recorded sign-in attempt (successful or failed) shown on the Security page.
 */
@Entity
@Table(name = "login_history", indexes = {
        @Index(name = "idx_login_history_user_time", columnList = "user_id, login_time")
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

    /** Owner of the attempt; null for attempts with an unknown identity. */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 60)
    private String browser;

    @Column(length = 60)
    private String os;

    @Column(name = "device_type", length = 16)
    private String deviceType;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(length = 80)
    private String location;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @PrePersist
    protected void onCreate() {
        if (this.loginTime == null) {
            this.loginTime = LocalDateTime.now();
        }
    }
}
