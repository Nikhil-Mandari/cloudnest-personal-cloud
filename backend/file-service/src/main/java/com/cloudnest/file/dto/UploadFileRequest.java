package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal request DTO carrying metadata derived from a multipart upload.
 * <p>
 * The controller populates the fields from the {@code MultipartFile} (original
 * name, content type, size) and the authenticated user context; the service
 * computes the object key, bucket, and SHA-256 checksum during upload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Metadata derived from a multipart file upload")
public class UploadFileRequest {

    @NotBlank(message = "Original file name must not be blank")
    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    @Schema(description = "Original file name (sanitised)", example = "report.pdf")
    private String originalFileName;

    @NotBlank(message = "Content type must not be blank")
    @Size(max = 100, message = "Content type must not exceed 100 characters")
    @Schema(description = "MIME content type of the uploaded file", example = "application/pdf")
    private String contentType;

    @NotNull(message = "File size must not be null")
    @Schema(description = "File size in bytes", example = "204800")
    private Long fileSize;

    @Size(max = 64, message = "Checksum must not exceed 64 characters")
    @Schema(description = "SHA-256 checksum (computed server-side during upload)", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    private String checksum;

    @NotNull(message = "Owner ID must not be null")
    @Schema(description = "ID of the owning user (from JWT context)", example = "1")
    private Long ownerId;

    @Schema(description = "Destination folder ID (UUID, null for root)", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private String folderId;

    @Builder.Default
    @Schema(description = "Whether the file should be publicly accessible", example = "false")
    private Boolean isPublic = false;
}
