package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.dto.EnableTwoFactorResponse;
import com.cloudnest.auth.dto.TwoFactorSetup;
import com.cloudnest.auth.dto.TwoFactorStatus;
import com.cloudnest.auth.entity.BackupCode;
import com.cloudnest.auth.entity.TwoFactorSettings;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.repository.BackupCodeRepository;
import com.cloudnest.auth.repository.TwoFactorSettingsRepository;
import com.cloudnest.auth.service.SecurityService;
import com.cloudnest.auth.service.TwoFactorService;
import com.cloudnest.auth.util.TotpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * TOTP-based two-factor authentication.
 * <p>
 * The setup secret is generated once and replaced on each new setup. Enabling
 * 2FA mints 10 single-use backup codes (stored as SHA-256 hashes). Disabling
 * accepts a TOTP code, an unused backup code or the account password.
 */
@Slf4j
@Service
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final String ISSUER = "CloudNest";
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int TOTP_WINDOW = 1;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final TwoFactorSettingsRepository settingsRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityService securityService;

    public TwoFactorServiceImpl(TwoFactorSettingsRepository settingsRepository,
                                BackupCodeRepository backupCodeRepository,
                                PasswordEncoder passwordEncoder,
                                @Lazy SecurityService securityService) {
        this.settingsRepository = settingsRepository;
        this.backupCodeRepository = backupCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityService = securityService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId) {
        return settingsRepository.findByUserId(userId)
                .map(TwoFactorSettings::getEnabled)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public TwoFactorStatus getStatus(Long userId) {
        boolean enabled = isEnabled(userId);
        long remaining = enabled
                ? backupCodeRepository.countByUserIdAndUsedFalse(userId)
                : 0;
        return TwoFactorStatus.builder()
                .enabled(enabled)
                .backupCodesRemaining(remaining)
                .build();
    }

    @Override
    @Transactional
    public TwoFactorSetup startSetup(Long userId, String email) {
        String secret = TotpUtil.generateSecret();
        String accountName = email != null && !email.isBlank() ? email : String.valueOf(userId);
        String otpauthUri = "otpauth://totp/"
                + urlEncode(ISSUER) + ":" + urlEncode(accountName)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(ISSUER)
                + "&algorithm=SHA1&digits=6&period=30";

        TwoFactorSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> TwoFactorSettings.builder().userId(userId).build());
        settings.setSecret(secret);
        settings.setEnabled(false);
        settingsRepository.save(settings);

        // Any stale unused backup codes no longer match this new secret.
        backupCodeRepository.deleteByUserId(userId);

        log.info("2FA setup started for userId={}", userId);
        return TwoFactorSetup.builder()
                .secret(secret)
                .otpauthUri(otpauthUri)
                .accountName(accountName)
                .issuer(ISSUER)
                .digits(6)
                .periodSeconds(30)
                .build();
    }

    @Override
    @Transactional
    public EnableTwoFactorResponse enable(Long userId, String code) {
        TwoFactorSettings settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Start 2FA setup before enabling it"));

        if (!TotpUtil.verify(settings.getSecret(), code, TOTP_WINDOW)) {
            throw new IllegalArgumentException("That code did not work. Check your authenticator app and try again.");
        }

        settings.setEnabled(true);
        settingsRepository.save(settings);

        List<String> codes = mintBackupCodes(userId);
        securityService.logEvent(userId, "2FA_ENABLED", "Two-factor authentication enabled");
        log.info("2FA enabled for userId={}", userId);
        return EnableTwoFactorResponse.builder().enabled(true).backupCodes(codes).build();
    }

    @Override
    @Transactional
    public void disable(Long userId, String verification, String accountPassword) {
        TwoFactorSettings settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Two-factor authentication is not set up"));

        boolean totpOk = TotpUtil.verify(settings.getSecret(), verification, TOTP_WINDOW);
        boolean backupOk = consumeBackupCode(userId, verification);
        boolean passwordOk = accountPassword != null && passwordEncoder.matches(verification, accountPassword);

        if (!totpOk && !backupOk && !passwordOk) {
            throw new IllegalArgumentException("Verification failed. Check your input and try again.");
        }

        settings.setEnabled(false);
        settingsRepository.save(settings);
        backupCodeRepository.deleteByUserId(userId);
        securityService.logEvent(userId, "2FA_DISABLED", "Two-factor authentication disabled");
        log.info("2FA disabled for userId={}", userId);
    }

    @Override
    @Transactional
    public List<String> regenerateBackupCodes(Long userId) {
        if (!isEnabled(userId)) {
            throw new IllegalStateException("Enable two-factor authentication first");
        }
        backupCodeRepository.deleteByUserId(userId);
        List<String> codes = mintBackupCodes(userId);
        securityService.logEvent(userId, "BACKUP_CODES_REGENERATED", "Backup codes regenerated");
        log.info("Backup codes regenerated for userId={}", userId);
        return codes;
    }

    @Override
    @Transactional
    public boolean verifyCode(Long userId, String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        Optional<TwoFactorSettings> settings = settingsRepository.findByUserId(userId);
        if (settings.isPresent() && Boolean.TRUE.equals(settings.get().getEnabled())
                && TotpUtil.verify(settings.get().getSecret(), code, TOTP_WINDOW)) {
            return true;
        }
        return consumeBackupCode(userId, code);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private List<String> mintBackupCodes(Long userId) {
        List<String> rawCodes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = randomBackupCode();
            rawCodes.add(code);
            backupCodeRepository.save(BackupCode.builder()
                    .userId(userId)
                    .codeHash(hashCode(code))
                    .build());
        }
        return rawCodes;
    }

    /** Consumes a backup code if it matches an unused stored hash; returns whether it matched. */
    private boolean consumeBackupCode(Long userId, String code) {
        return backupCodeRepository.findByUserIdAndCodeHash(userId, hashCode(code))
                .filter(c -> !Boolean.TRUE.equals(c.getUsed()))
                .map(c -> {
                    c.setUsed(true);
                    c.setUsedAt(LocalDateTime.now());
                    backupCodeRepository.save(c);
                    return true;
                })
                .orElse(false);
    }

    private String randomBackupCode() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(BACKUP_ALPHABET.charAt(RANDOM.nextInt(BACKUP_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String hashCode(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
