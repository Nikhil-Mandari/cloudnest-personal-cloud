package com.cloudnest.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Immutable audit trail entry recording a file-management action.
 * <p>
 * Every mutating (and download) operation on the File Service appends a row:
 * who (owner), what (action), on which resource, when, and from where
 * (IP + user agent when available).
 */
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the user whose action produced this log entry.
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * The audited action.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    /**
     * Resource kind (FILE / FOLDER / VERSION / SYSTEM).
     */
    @Column(name = "resource_type", length = 20)
    private String resourceType;

    /**
     * Resource identifier (file internal id, version id, etc.).
     */
    @Column(name = "resource_id", length = 64)
    private String resourceId;

    /**
     * Human-readable resource name (file name etc.).
     */
    @Column(name = "resource_name", length = 255)
    private String resourceName;

    /**
     * Optional free-form detail string.
     */
    @Column(name = "details", length = 1024)
    private String details;

    /**
     * Originating IP address (best effort).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Originating user agent (best effort).
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * Timestamp of the audited action.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * File-management actions that are recorded in the audit trail.
     */
    public enum AuditAction {
        UPLOAD,
        UPLOAD_REPLACED,
        UPLOAD_DUPLICATE_SKIPPED,
        DOWNLOAD,
        SHARE_DOWNLOAD,
        PREVIEW,
        RENAME,
        MOVE,
        DELETE,
        RESTORE,
        PERMANENT_DELETE,
        EMPTY_TRASH,
        FAVORITE_ADD,
        FAVORITE_REMOVE,
        VERSION_UPLOAD,
        VERSION_RESTORE,
        VERSION_DELETE,
        ZIP_DOWNLOAD
    }
}
