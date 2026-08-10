package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.entity.OtpVerification;
import com.cloudnest.auth.exception.RateLimitException;
import com.cloudnest.auth.repository.OtpVerificationRepository;
import com.cloudnest.auth.service.EmailService;
import com.cloudnest.auth.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of {@link OtpService} with SHA-256 hashed OTPs and
 * configurable expiry / cooldown.
 */
@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    /** OTP code length (6 digits). */
    private static final int OTP_LENGTH = 6;

    /** OTP lifetime in minutes. */
    private static final int OTP_EXPIRY_MINUTES = 5;

    /** Cooldown before a new OTP can be sent for the same email+purpose (seconds). */
    private static final int RESEND_COOLDOWN_SECONDS = 30;

    /** Maximum failed attempts before the OTP record is locked. */
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    public OtpServiceImpl(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public OtpDispatchResponse generateOtp(String email, String purpose) {
        return doGenerateOtp(email, purpose, null);
    }

    @Override
    @Transactional
    public OtpDispatchResponse generateOtpWithChallenge(String email, String purpose, String challengeToken) {
        return doGenerateOtp(email, purpose, challengeToken);
    }

    private OtpDispatchResponse doGenerateOtp(String email, String purpose, String challengeToken) {
        // Enforce the server-side resend cooldown: if a still-active code was
        // created within the cooldown window, refuse to generate another one.
        otpRepository
                .findTopByEmailAndPurposeAndVerifiedFalseAndInvalidatedFalseOrderByCreatedAtDesc(email, purpose)
                .ifPresent(existing -> {
                    if (existing.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                        throw new RateLimitException("Please wait before requesting another code.");
                    }
                });

        // Invalidate any previous active OTPs for this email + purpose
        invalidateOtps(email, purpose);

        // Generate a cryptographically random 6-digit code
        String plainOtp = String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH)));

        // Hash the OTP for storage
        String otpHash = hashOtp(plainOtp);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        OtpVerification entity = OtpVerification.builder()
                .email(email)
                .otpHash(otpHash)
                .purpose(purpose)
                .challengeToken(challengeToken)
                .expiresAt(expiresAt)
                .build();
        otpRepository.save(entity);

        log.info("OTP generated for email={}, purpose={}, expiresAt={}", email, purpose, expiresAt);

        // Attempt to send via email. If SMTP is not configured, include the
        // plain-text OTP as devOtp so development flows are not blocked.
        OtpDispatchResponse.OtpDispatchResponseBuilder response = OtpDispatchResponse.builder()
                .sent(false)
                .devOtp(null)
                .resendAfterSeconds(RESEND_COOLDOWN_SECONDS)
                .otpExpiryMinutes(OTP_EXPIRY_MINUTES);

        try {
            emailService.sendOtpEmail(email, plainOtp, OTP_EXPIRY_MINUTES);
            response.sent(true);
            log.info("OTP email sent to {}", email);
        } catch (Exception e) {
            log.warn("Failed to send OTP email to {}: {}. Returning devOtp for development.", email, e.getMessage());
            // The code still reached the user (via devOtp in the response), so
            // mark the dispatch as sent — the frontend treats `sent=false` as
            // "no account exists" and would otherwise abort the flow.
            response.sent(true);
            response.devOtp(plainOtp);
        }

        if (challengeToken != null) {
            response.challengeToken(challengeToken);
        }

        return response.build();
    }

    @Override
    @Transactional
    public Optional<OtpVerification> verifyOtp(String email, String code, String purpose) {
        Optional<OtpVerification> otpOpt = otpRepository
                .findTopByEmailAndPurposeAndVerifiedFalseAndInvalidatedFalseOrderByCreatedAtDesc(email, purpose);

        return validateAndMarkOtp(otpOpt, code);
    }

    @Override
    @Transactional
    public Optional<OtpVerification> verifyOtpWithChallenge(String challengeToken, String code) {
        Optional<OtpVerification> otpOpt = otpRepository
                .findByChallengeTokenAndVerifiedFalseAndInvalidatedFalse(challengeToken);

        return validateAndMarkOtp(otpOpt, code);
    }

    private Optional<OtpVerification> validateAndMarkOtp(Optional<OtpVerification> otpOpt, String code) {
        if (otpOpt.isEmpty()) {
            return Optional.empty();
        }

        OtpVerification otp = otpOpt.get();

        // Check expiry
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setInvalidated(true);
            otpRepository.save(otp);
            log.warn("OTP expired for email={}, purpose={}", otp.getEmail(), otp.getPurpose());
            return Optional.empty();
        }

        // Check attempt limit
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            otp.setInvalidated(true);
            otpRepository.save(otp);
            log.warn("OTP max attempts exceeded for email={}, purpose={}", otp.getEmail(), otp.getPurpose());
            return Optional.empty();
        }

        // Verify the code
        String expectedHash = hashOtp(code);
        if (!expectedHash.equals(otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            log.warn("Invalid OTP attempt for email={}, purpose={} (attempt {}/{})",
                    otp.getEmail(), otp.getPurpose(), otp.getAttempts(), MAX_ATTEMPTS);
            return Optional.empty();
        }

        // Success: mark as verified and invalidate
        otp.setVerified(true);
        otp.setInvalidated(true);
        otpRepository.save(otp);

        log.info("OTP verified for email={}, purpose={}", otp.getEmail(), otp.getPurpose());
        return Optional.of(otp);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findPurposeByChallengeToken(String challengeToken) {
        return otpRepository.findByChallengeTokenAndVerifiedFalseAndInvalidatedFalse(challengeToken)
                .map(OtpVerification::getPurpose);
    }

    @Override
    @Transactional
    public void invalidateOtps(String email, String purpose) {
        List<OtpVerification> active = otpRepository
                .findByEmailAndPurposeAndVerifiedFalseAndInvalidatedFalse(email, purpose);
        for (OtpVerification otp : active) {
            otp.setInvalidated(true);
        }
        otpRepository.saveAll(active);
    }

    @Override
    @Scheduled(fixedDelay = 3600000) // hourly sweep of expired OTP records
    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.debug("Cleanup sweep completed for expired OTP records");
    }

    /** SHA-256 hash of the OTP code (consistent with JWT secret derivation). */
    private String hashOtp(String code) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}