package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.OtpDispatchResponse;
import com.cloudnest.auth.entity.OtpVerification;

/**
 * Service interface for OTP generation and verification.
 */
public interface OtpService {

    /**
     * Generates a new OTP for the given email and purpose, persists the
     * hashed OTP, and returns its dispatch details (including the
     * plain-text OTP for development mode when email is unavailable).
     *
     * @param email   the recipient email
     * @param purpose the OTP purpose (REGISTRATION, LOGIN, PASSWORD_RESET)
     * @return an {@link OtpDispatchResponse} with cooldown/expiry info
     */
    OtpDispatchResponse generateOtp(String email, String purpose);

    /**
     * Generates a new OTP bound to an opaque challenge token (login /
     * password-reset flows).
     *
     * @param email          the recipient email
     * @param purpose        the OTP purpose
     * @param challengeToken UUID that binds the OTP to a specific challenge
     * @return an {@link OtpDispatchResponse} with cooldown/expiry info
     */
    OtpDispatchResponse generateOtpWithChallenge(String email, String purpose, String challengeToken);

    /**
     * Validates a plain-text OTP code against the stored hash.
     *
     * @param email the email the OTP was issued to
     * @param code  the plain-text code entered by the user
     * @param purpose the OTP purpose
     * @return the matching {@link OtpVerification} if valid, or empty
     */
    java.util.Optional<OtpVerification> verifyOtp(String email, String code, String purpose);

    /**
     * Validates an OTP code bound to a challenge token (login / password-reset).
     *
     * @param challengeToken the challenge UUID
     * @param code           the plain-text code entered by the user
     * @return the matching {@link OtpVerification} if valid, or empty
     */
    java.util.Optional<OtpVerification> verifyOtpWithChallenge(String challengeToken, String code);

    /**
     * Looks up the stored purpose of a challenge-bound OTP record
     * (used by the resend flow to preserve the purpose across resends).
     *
     * @param challengeToken the challenge UUID
     * @return the purpose (e.g. LOGIN, PASSWORD_RESET) if a record exists
     */
    java.util.Optional<String> findPurposeByChallengeToken(String challengeToken);

    /**
     * Invalidates all active OTPs for the given email and purpose
     * (used when resending or after successful verification).
     */
    void invalidateOtps(String email, String purpose);

    /** Deletes all expired OTP records (scheduled cleanup). */
    void cleanupExpiredOtps();
}