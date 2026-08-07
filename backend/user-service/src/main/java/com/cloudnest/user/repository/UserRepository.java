package com.cloudnest.user.repository;

import com.cloudnest.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the matching user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return an {@link Optional} containing the matching user, or empty if not found
     */
    Optional<User> findByEmail(String email);

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

    /**
     * Search users by username, display name, or email (case-insensitive).
     *
     * @param query the search term
     * @return a list of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "COALESCE(LOWER(u.displayName), '') LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> search(@Param("query") String query);

    /**
     * Paginated search used by the admin users view.
     *
     * @param query    the search term
     * @param pageable paging + sort instructions
     * @return a page of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "COALESCE(LOWER(u.displayName), '') LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchPage(@Param("query") String query, Pageable pageable);

    /**
     * Count users whose account is disabled (admin aggregate).
     */
    long countByEnabledFalse();

    /**
     * Count users with a given role (admin aggregate).
     */
    long countByRole(String role);

    /**
     * Count users created after a given instant (admin aggregate).
     */
    long countByCreatedAtAfter(LocalDateTime since);
}
