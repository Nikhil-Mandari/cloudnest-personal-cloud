package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Login-history entry shown on the Security page.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryResponse {

    private Long id;

    /** Owner of the sign-in attempt (populated on admin views). */
    private Long userId;

    private boolean success;
    private String ipAddress;
    private String browser;
    private String os;
    private String deviceType;
    private String deviceName;
    private String location;
    private String failureReason;
    private LocalDateTime loginTime;
}
