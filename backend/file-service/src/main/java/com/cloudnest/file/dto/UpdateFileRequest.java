package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating file metadata fields.
 * <p>
 * All fields are optional — only provided fields will be updated.
 * Renaming updates metadata only; the MinIO object key is never changed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Partial update request for file metadata (rename / visibility)")
public class UpdateFileRequest {

    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    @Schema(description = "New original file name (metadata only — MinIO object is not renamed)", example = "final-report.pdf")
    private String originalFileName;

    @Size(max = 100, message = "File type must not exceed 100 characters")
    @Schema(description = "New MIME content type", example = "application/pdf")
    private String fileType;

    @Schema(description = "Whether the file should be publicly accessible", example = "true")
    private Boolean isPublic;
}
