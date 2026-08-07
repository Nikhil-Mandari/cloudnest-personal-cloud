package com.cloudnest.auth.service;

import com.cloudnest.auth.client.NotificationServiceClient;
import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.dto.EnableTwoFactorResponse;
import com.cloudnest.auth.dto.RegenerateBackupCodesResponse;
import com.cloudnest.auth.dto.TwoFactorSetupResponse;
import com.cloudnest.auth.dto.TwoFactorStatusResponse;
import com.cloudnest.auth.entity.BackupCode;
import com.cloudnest.auth.entity.TwoFactorSetting;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.repository.BackupCodeRepository;
import com.cloudnest.auth.repository.TwoFactorSettingRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.security.ClientInfo;
import com.cloudnest.auth.security.TotpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TOTP two-factor-authentication orchestration.
 * <p>
 * <ul>
 *   <li><b>Setup</b> — issues a fresh base32 secret and the otpauth:// URI
 *       the frontend renders as a QR code (works with Google Authenticator,
 *       Microsoft Authenticator, Authy). Idempotent: re-requesting setup
 *       returns the same secret so an already-scanned QR is not invalidated.</li>
 *   <li><b>Enable</b> — only flips the flag after the user submits a valid
 *       code, proving they control the authenticator. Backup codes are
 *       generated once and returned in plaintext exactly once.</li>
 *   <li><b>Verify</b> — used at sign-in (TOTP or backup code). TOTP codes
 *       are checked against the last accepted time-step counter, so a
 *       captured code cannot be replayed within its validity window.</li>
 *   <li><b>Disable</b> — requires proof (TOTP, backup code or password)
 *       before the second factor is removed.</li>
 * </ul>
 * Backup codes are stored as SHA-256 hashes only; the plaintext is shown to
 * the user at generation time and never retrievable afterwards.
 */
@Slf4j
@Service
public class TwoFactorService {

    /** Backup-code alphabet excluding visually ambiguous characters. */
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final TwoFactorSettingRepository settingRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final UserCredentialRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties properties;
    private final SecurityEventService securityEvents;
    private final EmailService emailService;

    public TwoFactorService(TwoFactorSettingRepository settingRepository,
                            BackupCodeRepository backupCodeRepository,
                            UserCredentialRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            AuthProperties properties,
                            SecurityEventService securityEvents,
                            EmailService emailService) {
        this.settingRepository = settingRepository;
        this.backupCodeRepository = backupCodeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.securityEvents = securityEvents;
        this.emailService = emailService;
    }

    // ── Setup / enable / disable ─────────────────────────────────────────────

    /**
     * Returns (creating on first call) the TOTP secret and otpauth URI.
     */
    @Transactional
    public TwoFactorSetupResponse setup(Long userId) {
        UserCredential user = requireUser(userId);

        TwoFactorSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            setting = TwoFactorSetting.builder()
                    .userId(userId)
                    .secret(TotpUtils.generateSecret())
                    .enabled(false)
                    .build();
            setting = settingRepository.save(setting);
        }

        String uri = TotpUtils.generateOtpAuthUri(
                setting.getSecret(), user.getUsername(), properties.getWebauthn().getRpName());

        return TwoFactorSetupResponse.builder()
                .secret(setting.getSecret())
                .otpauthUri(uri)
                .accountName(user.getUsername())
                .issuer(properties.getWebauthn().getRpName())
                .digits(properties.getTwoFactor().getCodeDigits())
                .periodSeconds(properties.getTwoFactor().getTimeStepSeconds())
                .build();
    }

    /**
     * Enables 2FA after verifying a live authenticator code, then issues the
     * one-time backup codes.
     */
    @Transactional
    public EnableTwoFactorResponse enable(Long userId, String code, ClientInfo clientInfo) {
        TwoFactorSetting setting = requireSetting(userId);
        if (Boolean.TRUE.equals(setting.getEnabled())) {
            throw new IllegalArgumentException("Two-factor authentication is already enabled");
        }
        if (!verifyTotp(setting, code)) {
            throw new IllegalArgumentException(
                    "The code is invalid or expired. Check your authenticator app and try again.");
        }

        setting.setEnabled(true);
        setting.setEnabledAt(LocalDateTime.now());
        settingRepository.save(setting);

        List<String> backupCodes = generateAndStoreBackupCodes(userId);

        UserCredential user = requireUser(userId);
        securityEvents.log(user, SecurityEventService.ACTION_2FA_ENABLED, clientInfo,
                "Two-factor authentication enabled (TOTP)");
        emailService.sendTwoFactorEnabled(user.getEmail(), user.getUsername());
        securityEvents.notify(user, NotificationServiceClient.TYPE_TWO_FACTOR_ENABLED,
                "Two-factor authentication enabled",
                "Your account now requires a one-time code from your authenticator app when you sign in. "
                        + "Save your backup codes somewhere safe.");

        log.info("2FA enabled for userId={}", userId);
        return EnableTwoFactorResponse.builder()
                .enabled(true)
                .backupCodes(backupCodes)
                .build();
    }

    /**
     * Disables 2FA after verifying the caller controls the account (TOTP code,
     * unused backup code, or the account password). Backup codes are
     * invalidated with the setting.
     */
    @Transactional
    public void disable(Long userId, String verification, ClientInfo clientInfo) {
        TwoFactorSetting setting = requireSetting(userId);
        UserCredential user = requireUser(userId);

        if (!verifyDisableCredential(user, setting, verification)) {
            throw new IllegalArgumentException(
                    "Verification failed. Use your authenticator code, an unused backup code or your password.");
        }

        settingRepository.delete(setting);
        backupCodeRepository.deleteByUserId(userId);

        securityEvents.log(user, SecurityEventService.ACTION_2FA_DISABLED, clientInfo,
                "Two-factor authentication disabled");
        emailService.sendTwoFactorDisabled(user.getEmail(), user.getUsername());
        securityEvents.notify(user, NotificationServiceClient.TYPE_TWO_FACTOR_DISABLED,
                "Two-factor authentication disabled",
                "Two-factor authentication is now off. Consider re-enabling it to keep your account secure.");

        log.info("2FA disabled for userId={}", userId);
    }

    // ── Status / backup codes ────────────────────────────────────────────────

    /**
     * Current 2FA state: enabled flag + unused backup codes remaining.
     */
    @Transactional(readOnly = true)
    public TwoFactorStatusResponse status(Long userId) {
        TwoFactorSetting setting = settingRepository.findByUserId(userId).orElse(null);
        boolean enabled = setting != null && Boolean.TRUE.equals(setting.getEnabled());
        long remaining = enabled ? backupCodeRepository.countByUserIdAndUsedFalse(userId) : 0L;
        return TwoFactorStatusResponse.builder()
                .enabled(enabled)
                .backupCodesRemaining(remaining)
                .build();
    }

    /**
     * Invalidates all existing backup codes and issues a fresh set (returned
     * in plaintext exactly once).
     */
    @Transactional
    public RegenerateBackupCodesResponse regenerateBackupCodes(Long userId, ClientInfo clientInfo) {
        TwoFactorSetting setting = requireSetting(userId);
        if (!Boolean.TRUE.equals(setting.getEnabled())) {
            throw new IllegalArgumentException("Enable two-factor authentication before managing backup codes");
        }

        backupCodeRepository.deleteByUserId(userId);
        List<String> codes = generateAndStoreBackupCodes(userId);

        UserCredential user = requireUser(userId);
        securityEvents.log(user, SecurityEventService.ACTION_BACKUP_CODES_REGENERATED, clientInfo,
                "Backup codes regenerated (previous codes invalidated)");
        securityEvents.notify(user, NotificationServiceClient.TYPE_BACKUP_CODES_REGENERATED,
                "Backup codes regenerated",
                "Your previous backup codes were invalidated and a fresh set was generated. "
                        + "Store the new codes somewhere safe.");

        log.info("Backup codes regenerated for userId={}", userId);
        return RegenerateBackupCodesResponse.builder().backupCodes(codes).build();
    }

    // ── Verification (login + queries) ───────────────────────────────────────

    /**
     * Whether 2FA is currently enabled for the user.
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId) {
        return settingRepository.existsByUserIdAndEnabledTrue(userId);
    }

    /**
     * Verifies the second factor at sign-in: a live TOTP code (with replay
     * protection) or a single-use backup code (consumed on success).
     */
    @Transactional
    public boolean verifyForLogin(Long userId, String code) {
        TwoFactorSetting setting = settingRepository.findByUserId(userId).orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getEnabled())) {
            return false;
        }
        if (verifyTotp(setting, code)) {
            return true;
        }
        return consumeBackupCode(userId, code);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private TwoFactorSetting requireSetting(Long userId) {
        return settingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Two-factor authentication has not been set up for this account"));
    }

    private UserCredential requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    /**
     * Verifies a TOTP code and persists the accepted time-step counter so an
     * equal-or-earlier step is rejected afterwards (replay protection).
     */
    private boolean verifyTotp(TwoFactorSetting setting, String code) {
        int window = properties.getTwoFactor().getWindow();
        Optional<Long> counter = TotpUtils.findMatchingCounter(code, setting.getSecret(), window);
        if (counter.isEmpty()) {
            return false;
        }
        if (setting.getLastUsedCounter() != null && counter.get() <= setting.getLastUsedCounter()) {
            return false;
        }
        setting.setLastUsedCounter(counter.get());
        settingRepository.save(setting);
        return true;
    }

    private boolean consumeBackupCode(Long userId, String code) {
        String normalized = normalizeBackupCode(code);
        if (normalized.isEmpty()) {
            return false;
        }
        Optional<BackupCode> match =
                backupCodeRepository.findFirstByCodeHashAndUsedFalse(TotpUtils.sha256Hex(normalized));
        if (match.isEmpty()) {
            return false;
        }
        BackupCode backup = match.get();
        backup.setUsed(true);
        backup.setUsedAt(LocalDateTime.now());
        backupCodeRepository.save(backup);
        log.info("Backup code consumed for userId={}", userId);
        return true;
    }

    private boolean verifyDisableCredential(UserCredential user, TwoFactorSetting setting, String verification) {
        // Account password proves ownership.
        if (passwordEncoder.matches(verification, user.getPassword())) {
            return true;
        }
        // Live TOTP code (row is deleted right after, so no counter persists).
        if (TotpUtils.verify(verification, setting.getSecret(), properties.getTwoFactor().getWindow())) {
            return true;
        }
        // Unused backup code (consumed on success).
        return consumeBackupCode(user.getId(), verification);
    }

    private List<String> generateAndStoreBackupCodes(Long userId) {
        int count = properties.getTwoFactor().getBackupCodeCount();
        List<String> plaintext = new ArrayList<>(count);
        List<BackupCode> entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String raw = randomBackupCode();
            plaintext.add(formatBackupCode(raw));
            entities.add(BackupCode.builder()
                    .userId(userId)
                    .codeHash(TotpUtils.sha256Hex(raw))
                    .used(false)
                    .build());
        }
        backupCodeRepository.saveAll(entities);
        return plaintext;
    }

    private String randomBackupCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
        }
        return sb.toString();
    }

    private String formatBackupCode(String raw) {
        return raw.substring(0, 5) + "-" + raw.substring(5);
    }

    private String normalizeBackupCode(String code) {
        return code == null ? "" : code.replace("-", "").replace(" ", "").toUpperCase();
    }
}
