package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One recorded sign-in attempt (login history). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryEntry {

    private Long id;

    /** Owner of the attempt (present on admin views). */
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
