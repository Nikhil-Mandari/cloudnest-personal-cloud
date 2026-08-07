package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Credential snapshot returned after an admin enable/disable or role change.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private Boolean enabled;
    private String status;
    private LocalDateTime lastLoginAt;
}
