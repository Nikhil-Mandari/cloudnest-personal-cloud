package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Detailed response DTO for file metadata.
 * <p>
 * Returned when a single file record is requested or when listing files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detailed file metadata response")
public class FileResponse {

    @Schema(description = "Internal primary key", example = "1")
    private Long id;

    @Schema(description = "Public-facing file identifier (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String fileId;

    @Schema(description = "Original file name as uploaded by the user", example = "report.pdf")
    private String originalFileName;

    @Schema(description = "Unique object key inside MinIO (UUID_originalFileName)", example = "8f2a3b4c-..._report.pdf")
    private String objectName;

    @Schema(description = "MinIO bucket holding the object", example = "cloudnest-files")
    private String bucketName;

    @Schema(description = "Stored file name (legacy — equals objectName)", example = "8f2a3b4c-..._report.pdf")
    private String storedFileName;

    @Schema(description = "Canonical MIME content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "File type category (legacy — equals contentType)", example = "application/pdf")
    private String fileType;

    @Schema(description = "File size in bytes", example = "204800")
    private Long fileSize;

    @Schema(description = "Logical storage path (bucket/objectName)", example = "cloudnest-files/8f2a3b4c-..._report.pdf")
    private String storagePath;

    @Schema(description = "ID of the owning user", example = "1")
    private Long ownerId;

    @Schema(description = "ID of the folder the file belongs to (UUID, null for root)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String folderId;

    @Schema(description = "Whether the file is publicly accessible", example = "false")
    private Boolean isPublic;

    @Schema(description = "Whether the owner marked the file as a favorite", example = "false")
    private Boolean isFavorite;

    @Schema(description = "SHA-256 checksum of the file content", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String checksum;

    @Schema(description = "Lifecycle status (ACTIVE / DELETED)", example = "ACTIVE")
    private String status;

    @Schema(description = "Virus scan status (PENDING / SCANNING / CLEAN / INFECTED / ERROR)", example = "CLEAN")
    private String scanStatus;

    @Schema(description = "Timestamp when the file was uploaded", example = "2026-08-03T10:15:30")
    private LocalDateTime uploadedAt;

    @Schema(description = "Timestamp when the record was created", example = "2026-08-03T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the record was last updated", example = "2026-08-03T10:15:30")
    private LocalDateTime updatedAt;
}
