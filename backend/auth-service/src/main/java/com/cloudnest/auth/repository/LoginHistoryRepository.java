package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserId(Long userId, Pageable pageable);

    long countByUserIdAndSuccessTrue(Long userId);

    long countByUserIdAndSuccessFalseAndLoginTimeAfter(Long userId, LocalDateTime after);

    LoginHistory findFirstByUserIdAndSuccessTrueOrderByLoginTimeDesc(Long userId);
}
