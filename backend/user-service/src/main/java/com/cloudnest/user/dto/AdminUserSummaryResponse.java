package com.cloudnest.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Platform-wide user aggregates (admin overview).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummaryResponse {

    private long totalUsers;

    private long activeUsers;

    private long disabledUsers;

    private long adminUsers;

    private long newLast7Days;
}
