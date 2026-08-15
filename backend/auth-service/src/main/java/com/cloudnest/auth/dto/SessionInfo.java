package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * An active sign-in session shown on the Security page (derived from the
 * user's non-revoked refresh tokens).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInfo {

    private String sessionId;
    private String deviceId;
    private String deviceName;
    private String browser;
    private String os;
    private String deviceType;
    private String ipAddress;
    private String location;
    private boolean current;
    private boolean trusted;
    private LocalDateTime loginTime;
    private LocalDateTime lastActive;
}
