package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.EnableTwoFactorResponse;
import com.cloudnest.auth.dto.TwoFactorSetup;
import com.cloudnest.auth.dto.TwoFactorStatus;

import java.util.List;

/**
 * Two-factor authentication (TOTP + backup codes) operations.
 */
public interface TwoFactorService {

    /** Whether 2FA is enabled for the user. */
    boolean isEnabled(Long userId);

    TwoFactorStatus getStatus(Long userId);

    /** Starts setup: generates a fresh TOTP secret (existing one is replaced). */
    TwoFactorSetup startSetup(Long userId, String email);

    /** Enables 2FA after verifying a code from the setup secret; returns backup codes once. */
    EnableTwoFactorResponse enable(Long userId, String code);

    /** Disables 2FA; {@code verification} may be a TOTP code, unused backup code or password. */
    void disable(Long userId, String verification, String accountPassword);

    List<String> regenerateBackupCodes(Long userId);

    /**
     * Verifies a TOTP code or consumes an unused backup code.
     *
     * @return {@code true} when the code is valid
     */
    boolean verifyCode(Long userId, String code);
}
