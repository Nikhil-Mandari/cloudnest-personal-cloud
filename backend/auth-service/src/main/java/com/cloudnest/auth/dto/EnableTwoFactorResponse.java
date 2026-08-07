package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Result of enabling 2FA: confirms activation and returns the freshly
 * generated backup codes. The codes are only ever returned here — the user
 * must save them now.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnableTwoFactorResponse {

    private boolean enabled;

    /** Plaintext backup codes (shown exactly once). */
    private List<String> backupCodes;
}
