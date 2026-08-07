package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.SecurityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SecurityLog} operations.
 */
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    Page<SecurityLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** All users' security logs, newest first (admin view). */
    Page<SecurityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
