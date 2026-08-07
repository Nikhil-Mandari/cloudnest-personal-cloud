package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A single entry in the "largest files" analytics list.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "One of the user's largest files")
public class LargestFileInfo {

    @Schema(description = "Internal primary key", example = "1")
    private Long id;

    @Schema(description = "File name", example = "backup.iso")
    private String originalFileName;

    @Schema(description = "MIME content type", example = "application/octet-stream")
    private String contentType;

    @Schema(description = "File size in bytes", example = "734003200")
    private Long fileSize;

    @Schema(description = "Upload timestamp", example = "2026-07-01T08:00:00")
    private LocalDateTime uploadedAt;
}
