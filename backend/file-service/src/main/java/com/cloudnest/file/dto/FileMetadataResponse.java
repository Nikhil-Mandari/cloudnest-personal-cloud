package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for file metadata listings.
 * <p>
 * Contains only the most commonly accessed fields, suitable for
 * directory listings and search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Lightweight file metadata response used for listings and search results")
public class FileMetadataResponse {

    @Schema(description = "Internal primary key (used by mutation endpoints)", example = "1")
    private Long id;

    @Schema(description = "Public-facing file identifier (UUID)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String fileId;

    @Schema(description = "Original file name", example = "report.pdf")
    private String originalFileName;

    @Schema(description = "File type category (MIME type)", example = "application/pdf")
    private String fileType;

    @Schema(description = "File size in bytes", example = "204800")
    private Long fileSize;

    @Schema(description = "ID of the owning user", example = "1")
    private Long ownerId;

    @Schema(description = "ID of the folder the file belongs to (UUID, null for root)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String folderId;

    @Schema(description = "Whether the file is publicly accessible", example = "false")
    private Boolean isPublic;

    @Schema(description = "Whether the owner marked the file as a favorite", example = "false")
    private Boolean isFavorite;

    @Schema(description = "Lifecycle status (ACTIVE / DELETED)", example = "ACTIVE")
    private String status;

    @Schema(description = "Timestamp when the record was created", example = "2026-08-03T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when the record was last updated", example = "2026-08-03T10:15:30")
    private LocalDateTime updatedAt;
}
