package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Aggregated security posture for the Security page: score, account state,
 * device/session counts and recent activity signals.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityOverviewResponse {

    /** 0–100 heuristic score of the account's security posture. */
    private int securityScore;

    private String accountStatus;
    private boolean emailVerified;

    /** Whether TOTP two-factor authentication is enabled. */
    private boolean twoFactorEnabled;

    /** Number of registered WebAuthn passkeys. */
    private int passkeyCount;

    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private int activeSessionCount;
    private int trustedDeviceCount;
    private long failedLoginsLast7Days;
    private long totalLogins;
}
