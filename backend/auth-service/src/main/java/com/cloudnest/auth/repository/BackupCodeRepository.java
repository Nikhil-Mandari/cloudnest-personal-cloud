package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {

    List<BackupCode> findByUserId(Long userId);

    List<BackupCode> findByUserIdAndUsedFalse(Long userId);

    Optional<BackupCode> findByUserIdAndCodeHash(Long userId, String codeHash);

    long countByUserIdAndUsedFalse(Long userId);

    void deleteByUserId(Long userId);
}
