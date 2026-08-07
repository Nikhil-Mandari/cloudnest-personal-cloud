package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for single-use {@link BackupCode} rows.
 */
@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {

    List<BackupCode> findByUserId(Long userId);

    long countByUserIdAndUsedFalse(Long userId);

    Optional<BackupCode> findFirstByCodeHashAndUsedFalse(String codeHash);

    void deleteByUserId(Long userId);
}
