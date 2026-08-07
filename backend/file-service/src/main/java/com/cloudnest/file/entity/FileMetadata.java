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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Represents metadata for a file stored in the CloudNest platform.
 * <p>
 * This entity manages file metadata only — the actual binary content is stored
 * in MinIO object storage, referenced by {@code objectName} within
 * {@code bucketName}. The SHA-256 {@code checksum} is captured at upload time
 * for integrity verification and future duplicate detection.
 * Supports soft-delete via the {@code status} field: files moved to the trash
 * keep their MinIO object until restored or permanently deleted.
 */
@Entity
@Table(name = "file_metadata")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    /**
     * Internal auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public-facing unique file identifier (UUID).
     * Used in API responses and external references.
     */
    @Column(name = "file_id", nullable = false, unique = true, updatable = false, length = 36)
    private String fileId;

    /**
     * Original file name as provided by the user at upload time.
     */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /**
     * Name under which the file is stored on the storage backend.
     * Equal to {@code objectName} — retained for backward compatibility.
     */
    @Column(name = "stored_file_name", nullable = false, unique = true, length = 512)
    private String storedFileName;

    /**
     * Unique object key inside MinIO, generated as {@code UUID_originalFileName}.
     */
    @Column(name = "object_name", nullable = false, unique = true, length = 512)
    private String objectName;

    /**
     * MinIO bucket that holds the object.
     */
    @Column(name = "bucket_name", nullable = false, length = 255)
    private String bucketName;

    /**
     * MIME type or file extension category (e.g. "image/png", "application/pdf").
     * Retained for backward compatibility with earlier versions.
     */
    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    /**
     * Canonical MIME content type of the file (e.g. "application/pdf").
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Storage path relative to the storage backend root (bucket + object key).
     */
    @Column(name = "storage_path", nullable = false, length = 1024)
    private String storagePath;

    /**
     * ID of the user who owns this file.
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * ID of the folder this file belongs to (UUID; nullable for root-level files).
     */
    @Column(name = "folder_id", length = 36)
    private String folderId;

    /**
     * Whether this file is publicly accessible without authentication.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Whether the owner has marked this file as a favorite.
     */
    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private Boolean isFavorite = false;

    /**
     * SHA-256 checksum of the file content, computed at upload time.
     */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /**
     * Current lifecycle status of the file.
     * <ul>
     *   <li>{@code ACTIVE} — file is available for access</li>
     *   <li>{@code DELETED} — file is in the trash (soft-deleted, can be restored)</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FileStatus status = FileStatus.ACTIVE;

    /**
     * Current virus-scan lifecycle status.
     * <p>
     * Set at upload time by the configured scanner (Noop or ClamAV). Files
     * flagged {@code INFECTED} are blocked from download / preview.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", length = 20)
    @Builder.Default
    private ScanStatus scanStatus = ScanStatus.CLEAN;

    /**
     * Timestamp when the file content was uploaded to MinIO.
     */
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /**
     * Timestamp when the file metadata record was created.
     * Managed automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the file metadata record was last updated.
     * Managed automatically by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum representing the lifecycle status of a file record.
     */
    public enum FileStatus {
        ACTIVE,
        DELETED
    }
}
