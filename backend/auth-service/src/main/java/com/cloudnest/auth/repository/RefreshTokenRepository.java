package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds an active (non-revoked) refresh token by its SHA-256 hash.
     */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /**
     * Finds a token by its unique jti.
     */
    Optional<RefreshToken> findByJti(String jti);

    /**
     * All refresh tokens belonging to a session.
     */
    List<RefreshToken> findBySessionId(String sessionId);

    /**
     * All active refresh tokens belonging to a user.
     */
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /**
     * Atomically revokes a token only if it is still active (optimistic
     * compare-and-swap). Returns {@code 1} when this call won the race and
     * revoked the token, {@code 0} when it was already revoked (rotation
     * reuse detected).
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken t
               SET t.revoked = true, t.revokedAt = :revokedAt
             WHERE t.id = :id AND t.revoked = false
            """)
    int revokeIfActive(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt);
}
