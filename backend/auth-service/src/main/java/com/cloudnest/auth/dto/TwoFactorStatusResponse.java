package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Current 2FA state for the Security page: enabled flag plus how many unused
 * backup codes remain.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorStatusResponse {

    private boolean enabled;

    /** Number of unused backup codes (0 when 2FA is disabled). */
    private long backupCodesRemaining;
}
