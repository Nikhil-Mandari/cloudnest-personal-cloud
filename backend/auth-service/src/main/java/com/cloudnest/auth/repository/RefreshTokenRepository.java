package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} entities.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Finds a non-revoked token by its hash. */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /** Revokes every token belonging to a user (logout-all). */
    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    /** Deletes expired tokens (cleanup sweep). */
    void deleteByExpiresAtBefore(LocalDateTime now);
}