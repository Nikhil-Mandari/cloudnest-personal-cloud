package com.cloudnest.file.service;

import com.cloudnest.file.dto.PagedAuditLogsResponse;
import com.cloudnest.file.entity.AuditLog.AuditAction;

/**
 * Service for the immutable file-activity audit trail.
 */
public interface AuditLogService {

    /**
     * Appends an entry to the audit trail. Failures are logged and swallowed —
     * auditing must never break the audited operation.
     *
     * @param ownerId      the user whose action is recorded
     * @param action       the audited action
     * @param resourceType resource kind (FILE / VERSION / …), may be null
     * @param resourceId   resource identifier, may be null
     * @param resourceName human-readable resource name, may be null
     * @param details      optional free-form detail, may be null
     */
    void record(Long ownerId, AuditAction action, String resourceType,
                String resourceId, String resourceName, String details);

    /**
     * Returns a page of audit entries for an owner, newest first.
     *
     * @param ownerId the user whose trail is queried
     * @param page    zero-based page index
     * @param size    page size (clamped to 1..100)
     * @param action  optional action filter (null/blank = all actions)
     * @return a paged audit response
     */
    PagedAuditLogsResponse getLogs(Long ownerId, int page, int size, String action);

    /**
     * Returns a page of audit entries across every owner, newest first
     * (admin view). The owner can be narrowed with {@code userId}.
     *
     * @param page   zero-based page index
     * @param size   page size (clamped to 1..100)
     * @param action optional action filter (null/blank = all actions)
     * @param userId optional owner filter (null = all users)
     * @return a paged audit response
     */
    PagedAuditLogsResponse getAllLogs(int page, int size, String action, Long userId);
}
