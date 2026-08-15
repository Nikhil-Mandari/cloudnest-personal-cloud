package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.SecurityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    Page<SecurityLog> findByUserId(Long userId, Pageable pageable);
}
