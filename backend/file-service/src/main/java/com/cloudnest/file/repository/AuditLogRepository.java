package com.cloudnest.file.repository;

import com.cloudnest.file.entity.AuditLog;
import com.cloudnest.file.entity.AuditLog.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AuditLog} entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Paged audit trail for an owner. Ordering is supplied through the
     * {@link Pageable} (newest first).
     */
    Page<AuditLog> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Paged audit trail for an owner filtered by action. Ordering is supplied
     * through the {@link Pageable} (newest first).
     */
    Page<AuditLog> findByOwnerIdAndAction(Long ownerId, AuditAction action, Pageable pageable);

    // ── Admin / platform-wide queries ────────────────────────────────────────

    /**
     * Paged audit trail across every owner, filtered by action (admin view).
     * Ordering is supplied through the {@link Pageable} (newest first).
     */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);
}
