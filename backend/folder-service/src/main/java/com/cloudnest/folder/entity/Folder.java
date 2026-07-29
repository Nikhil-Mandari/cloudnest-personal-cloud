package com.cloudnest.folder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a folder in the CloudNest platform.
 * <p>
 * Supports unlimited nesting via the {@code parentFolderId} self-referencing
 * relationship. The {@code path} field maintains the full hierarchical path
 * (e.g. "/Documents/Java/Spring") for efficient querying.
 * Soft-delete is supported via the {@code deleted} flag.
 */
@Entity
@Table(name = "folders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    /**
     * Primary key — generated as a UUID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /**
     * Display name of the folder.
     */
    @NotBlank(message = "Folder name must not be blank")
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * ID of the user who owns this folder.
     */
    @Column(name = "owner_id", nullable = false, length = 36)
    private UUID ownerId;

    /**
     * ID of the parent folder (nullable for root-level folders).
     */
    @Column(name = "parent_folder_id", length = 36)
    private UUID parentFolderId;

    /**
     * Full hierarchical path of this folder.
     * <p>
     * Examples:
     * <ul>
     *   <li>Root folder "Documents": {@code /Documents}</li>
     *   <li>Child "Java": {@code /Documents/Java}</li>
     *   <li>Grandchild "Spring": {@code /Documents/Java/Spring}</li>
     * </ul>
     */
    @Column(name = "path", nullable = false, length = 2048)
    private String path;

    /**
     * Depth level in the folder hierarchy (0-indexed).
     * <ul>
     *   <li>{@code 0} — root folder (no parent)</li>
     *   <li>{@code 1} — direct child of a root folder</li>
     *   <li>{@code N} — descendant at depth N</li>
     * </ul>
     */
    @Column(name = "level", nullable = false)
    @Builder.Default
    private Integer level = 0;

    /**
     * Soft-delete flag.
     * <p>
     * When {@code true}, the folder is considered deleted and should be
     * excluded from normal queries. Child folders are recursively flagged
     * when a parent is deleted.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    /**
     * Timestamp when the folder record was created.
     * Managed automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the folder record was last updated.
     * Managed automatically by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.deleted == null) {
            this.deleted = false;
        }
        if (this.level == null) {
            this.level = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
