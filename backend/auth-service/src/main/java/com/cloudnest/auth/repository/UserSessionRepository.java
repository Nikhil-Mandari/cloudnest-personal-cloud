package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link UserSession} operations.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findByUserIdAndActiveTrueOrderByLastActiveDesc(Long userId);

    Optional<UserSession> findBySessionId(String sessionId);

    Optional<UserSession> findBySessionIdAndActiveTrue(String sessionId);

    Optional<UserSession> findByUserIdAndSessionId(Long userId, String sessionId);

    /**
     * Any session (active or ended) matching the user + device id.
     */
    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    /** Active sessions across all users (admin aggregate). */
    long countByActiveTrue();
}
