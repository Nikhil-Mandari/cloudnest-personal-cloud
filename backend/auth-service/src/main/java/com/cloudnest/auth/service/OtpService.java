package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.OtpVerification;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.OtpExpiredException;
import com.cloudnest.auth.exception.OtpInvalidException;
import com.cloudnest.auth.exception.OtpMaxAttemptsException;
import com.cloudnest.auth.exception.OtpResendCooldownException;
import com.cloudnest.auth.repository.OtpVerificationRepository;
import com.cloudnest.auth.util.Hashing;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Issues and verifies one-time passcodes.
 * <p>
 * Codes are six random digits, stored as SHA-256 hashes, expire after
 * {@code auth.otp.expiry-minutes}, allow at most {@code auth.otp.max-attempts}
 * verification attempts, and can only be resent after the configured cooldown.
 */
@Slf4j
@Service
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpVerificationRepository otpRepository,
                      EmailService emailService,
                      AuthProperties properties) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
        this.properties = properties;
    }

    /**
     * Result of dispatching an OTP.
     *
     * @param devOtp              the plain code, only when email delivery is
     *                            disabled (development) — {@code null} otherwise
     * @param resendAfterSeconds  cooldown before another code can be requested
     */
    public record OtpDispatchResult(String devOtp, long resendAfterSeconds) {
    }

    /**
     * Generates (or resends) an OTP for the user + purpose and emails it.
     * <p>
     * Resends are rejected while the cooldown is active.
     */
    @Transactional
    public OtpDispatchResult generateAndSend(UserCredential user, OtpVerification.Purpose purpose) {
        OtpVerification existing = otpRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(user.getId(), purpose)
                .orElse(null);

        if (existing != null && !isExpired(existing) && existing.getResentAt() != null) {
            long remaining = cooldownRemaining(existing);
            if (remaining > 0) {
                throw new OtpResendCooldownException(remaining);
            }
        }

        String code = generateCode();
        LocalDateTime now = LocalDateTime.now();

        OtpVerification otp = OtpVerification.builder()
                .userId(user.getId())
                .purpose(purpose)
                .codeHash(Hashing.hmacSha256Hex(code, properties.getOtp().getPepper()))
                .expiresAt(now.plusMinutes(properties.getOtp().getExpiryMinutes()))
                .attempts(0)
                .maxAttempts(properties.getOtp().getMaxAttempts())
                .requestedAt(now)
                .resentAt(now)
                .build();
        otpRepository.save(otp);

        // Prune stale records so the table stays tidy.
        prune(user.getId(), purpose);

        emailService.sendOtp(user.getEmail(), user.getUsername(), code,
                purposeLabel(purpose), properties.getOtp().getExpiryMinutes());

        String devOtp = emailService.isEnabled() ? null : code;
        log.info("OTP {} issued for userId={} (email delivered: {})",
                purpose, user.getId(), emailService.isEnabled());

        return new OtpDispatchResult(devOtp, properties.getOtp().getResendCooldownSeconds());
    }

    /**
     * Verifies the submitted code for the user + purpose.
     * <p>
     * Invalid attempts consume an attempt; exceeding {@code max-attempts}
     * invalidates the record. Verified records are consumed atomically.
     *
     * @return the verified user
     */
    @Transactional
    public UserCredential verify(UserCredential user, OtpVerification.Purpose purpose, String code) {
        if (code == null || code.isBlank()) {
            throw new OtpInvalidException("Verification code is required");
        }

        OtpVerification otp = otpRepository
                .findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new OtpInvalidException("No active verification code. Request a new one."));

        if (isExpired(otp)) {
            otp.setConsumed(true);
            otpRepository.save(otp);
            throw new OtpExpiredException("This code has expired. Request a new one.");
        }

        if (otp.getAttempts() >= otp.getMaxAttempts()) {
            otp.setConsumed(true);
            otpRepository.save(otp);
            throw new OtpMaxAttemptsException("Too many incorrect attempts. Request a new code.");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!Hashing.constantTimeEquals(otp.getCodeHash(),
                Hashing.hmacSha256Hex(code, properties.getOtp().getPepper()))) {
            otpRepository.save(otp);
            throw new OtpInvalidException("Incorrect code. " + (otp.getMaxAttempts() - otp.getAttempts())
                    + " attempt(s) remaining.");
        }

        otp.setVerified(true);
        otp.setConsumed(true);
        otpRepository.save(otp);

        log.info("OTP {} verified for userId={}", purpose, user.getId());
        return user;
    }

    /**
     * Cooldown remaining (seconds) before the user can request another code.
     */
    public long cooldownRemaining(OtpVerification otp) {
        if (otp == null || otp.getResentAt() == null) {
            return 0;
        }
        long cooldown = properties.getOtp().getResendCooldownSeconds();
        long elapsed = Duration.between(otp.getResentAt(), LocalDateTime.now()).getSeconds();
        return Math.max(0, cooldown - elapsed);
    }

    // -- Private helpers -----------------------------------------------------

    private String generateCode() {
        int length = properties.getOtp().getLength();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private boolean isExpired(OtpVerification otp) {
        return otp.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private String purposeLabel(OtpVerification.Purpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> "Account activation";
            case LOGIN -> "Sign-in verification";
            case PASSWORD_RESET -> "Password reset";
        };
    }

    private void prune(Long userId, OtpVerification.Purpose purpose) {
        List<OtpVerification> stale = otpRepository.findByUserIdAndPurpose(userId, purpose);
        stale.stream()
                .filter(o -> Boolean.TRUE.equals(o.getConsumed())
                        || o.getExpiresAt().isBefore(LocalDateTime.now().minusDays(1)))
                .forEach(otpRepository::delete);
    }
}
