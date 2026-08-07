package com.cloudnest.auth.repository;

import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.entity.UserCredential.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link UserCredential} entity operations.
 */
@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    /**
     * Find a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the matching user, or empty if not found
     */
    Optional<UserCredential> findByUsername(String username);

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the matching user, or empty if not found
     */
    Optional<UserCredential> findByEmail(String email);

    /**
     * Check whether a username is already taken.
     *
     * @param username the username to check
     * @return {@code true} if a user with the given username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check whether an email address is already registered.
     *
     * @param email the email to check
     * @return {@code true} if a user with the given email exists
     */
    boolean existsByEmail(String email);

    // ── Admin aggregates ────────────────────────────────────────────────────

    long countByStatus(AccountStatus status);

    long countByEnabled(Boolean enabled);

    long countByRole(String role);
}
