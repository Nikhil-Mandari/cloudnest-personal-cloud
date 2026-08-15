package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aggregated security posture for the Security page overview. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityOverview {

    private int securityScore;

    /** PENDING_VERIFICATION / ACTIVE / LOCKED. */
    private String accountStatus;

    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private long activeSessionCount;
    private long trustedDeviceCount;
    private long failedLoginsLast7Days;
    private long totalLogins;
}
