package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One security-log event shown on the Security page. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityLogEntry {

    private Long id;

    /** Owner of the event (present on admin views). */
    private Long userId;

    private String action;
    private String details;
    private String ipAddress;
    private String browser;
    private String os;
    private String location;
    private LocalDateTime createdAt;
}
