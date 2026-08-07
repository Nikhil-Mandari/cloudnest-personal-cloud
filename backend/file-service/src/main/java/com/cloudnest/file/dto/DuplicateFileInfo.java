package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight description of an existing file that has identical content
 * (same SHA-256 checksum) to a new upload — used to drive the client-side
 * duplicate resolution dialog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Existing file with identical content to a new upload")
public class DuplicateFileInfo {

    @Schema(description = "Internal primary key of the existing file", example = "4")
    private Long id;

    @Schema(description = "Public-facing file identifier (UUID)", example = "7c9e6679-…")
    private String fileId;

    @Schema(description = "File name of the existing file", example = "report.pdf")
    private String originalFileName;

    @Schema(description = "Existing file size in bytes", example = "204800")
    private Long fileSize;

    @Schema(description = "SHA-256 checksum shared by both files", example = "e3b0c442…")
    private String checksum;

    @Schema(description = "Folder the existing file lives in (null = root)", example = "7c9e6679-…")
    private String folderId;

    @Schema(description = "When the existing file was uploaded", example = "2026-08-03T10:15:30")
    private LocalDateTime uploadedAt;
}
