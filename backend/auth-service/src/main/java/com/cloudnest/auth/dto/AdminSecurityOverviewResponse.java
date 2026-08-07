package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Platform-wide security posture (admin view): account mix, login volume and
 * active-session / trusted-device totals across every user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSecurityOverviewResponse {

    private long totalAccounts;
    private long activeAccounts;
    private long lockedAccounts;
    private long pendingVerification;
    private long disabledAccounts;
    private long adminCount;

    private long totalLogins;
    private long failedLoginsLast7Days;

    private long activeSessions;
    private long trustedDeviceCount;
}
