package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for an archived file version snapshot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Archived file version snapshot")
public class FileVersionResponse {

    @Schema(description = "Version record primary key", example = "10")
    private Long id;

    @Schema(description = "Sequential version number", example = "2")
    private Integer versionNumber;

    @Schema(description = "Snapshot size in bytes", example = "204800")
    private Long fileSize;

    @Schema(description = "MIME content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "SHA-256 checksum of the snapshot content", example = "e3b0c442…")
    private String checksum;

    @Schema(description = "ID of the user who created the version", example = "1")
    private Long uploadedBy;

    @Schema(description = "Optional user note", example = "Reviewed draft")
    private String note;

    @Schema(description = "Timestamp when the version was archived", example = "2026-08-04T09:12:00")
    private LocalDateTime createdAt;
}
