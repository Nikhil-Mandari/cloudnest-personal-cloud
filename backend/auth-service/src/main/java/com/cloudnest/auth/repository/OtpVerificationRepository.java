package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link OtpVerification} entities.
 */
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Finds the latest non-verified, non-invalidated OTP for a given email and purpose.
     */
    Optional<OtpVerification> findTopByEmailAndPurposeAndVerifiedFalseAndInvalidatedFalseOrderByCreatedAtDesc(
            String email, String purpose);

    /**
     * Finds a non-expired, non-verified OTP by challenge token.
     */
    Optional<OtpVerification> findByChallengeTokenAndVerifiedFalseAndInvalidatedFalse(String challengeToken);

    /**
     * Invalidates (marks as invalidated) all active OTPs for an email + purpose.
     */
    List<OtpVerification> findByEmailAndPurposeAndVerifiedFalseAndInvalidatedFalse(String email, String purpose);

    /**
     * Deletes all expired OTPs (cleanup sweep).
     */
    void deleteByExpiresAtBefore(LocalDateTime now);
}