package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link OtpVerification} operations.
 */
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Finds the most recent unconsumed OTP for a user + purpose.
     */
    Optional<OtpVerification> findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
            Long userId, OtpVerification.Purpose purpose);

    /**
     * Lists all records for a user + purpose (used to prune stale records).
     */
    List<OtpVerification> findByUserIdAndPurpose(Long userId, OtpVerification.Purpose purpose);
}
