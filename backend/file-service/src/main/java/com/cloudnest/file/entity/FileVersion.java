package com.cloudnest.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
 * An archived content snapshot of a {@link FileMetadata} record.
 * <p>
 * The <em>current</em> content of a file always lives on the file record
 * itself ({@code objectName}); every time the content is replaced (a new
 * version is uploaded, or a duplicate is resolved with REPLACE) the previous
 * content is archived here as a version row. Restoring a version swaps the
 * file's current object pointer back to that snapshot.
 */
@Entity
@Table(name = "file_versions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK to the owning {@link FileMetadata} record ({@code file_metadata.id}).
     */
    @Column(name = "file_metadata_id", nullable = false)
    private Long fileMetadataId;

    /**
     * Sequential version number. The current content implicitly carries the
     * next number; this row is the snapshot that <em>was</em> current when it
     * was archived.
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /**
     * MinIO object key of this snapshot's binary content.
     */
    @Column(name = "object_name", nullable = false, length = 512)
    private String objectName;

    /**
     * Stored file name at archive time (equals the object name).
     */
    @Column(name = "stored_file_name", nullable = false, length = 512)
    private String storedFileName;

    /**
     * Snapshot size in bytes.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * SHA-256 checksum of the snapshot content.
     */
    @Column(name = "checksum", length = 64)
    private String checksum;

    /**
     * MIME content type of the snapshot.
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * ID of the user who created this version.
     */
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    /**
     * Optional user note about the version.
     */
    @Column(name = "note", length = 512)
    private String note;

    /**
     * Timestamp when the version was archived.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
