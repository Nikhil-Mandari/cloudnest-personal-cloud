package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link LoginHistory} operations.
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserIdOrderByLoginTimeDesc(Long userId, Pageable pageable);

    /**
     * Total number of recorded sign-in attempts for the user.
     */
    long countByUserId(Long userId);

    /**
     * Failed attempts since the given timestamp (drives the security score).
     */
    long countByUserIdAndSuccessFalseAndLoginTimeAfter(Long userId, java.time.LocalDateTime after);

    // ── Admin aggregates (all users) ────────────────────────────────────────

    Page<LoginHistory> findAllByOrderByLoginTimeDesc(Pageable pageable);

    long countBySuccessFalseAndLoginTimeAfter(java.time.LocalDateTime after);
}
