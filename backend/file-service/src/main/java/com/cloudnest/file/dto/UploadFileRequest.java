package com.cloudnest.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for uploading file metadata.
 * <p>
 * Captures the metadata required to register a new file record.
 * The actual binary upload is handled separately in a later phase.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileRequest {

    @NotBlank(message = "Original file name must not be blank")
    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    @NotBlank(message = "File type must not be blank")
    @Size(max = 100, message = "File type must not exceed 100 characters")
    private String fileType;

    @NotNull(message = "File size must not be null")
    private Long fileSize;

    @NotBlank(message = "Storage path must not be blank")
    @Size(max = 512, message = "Storage path must not exceed 512 characters")
    private String storagePath;

    @NotNull(message = "Owner ID must not be null")
    private Long ownerId;

    private Long folderId;

    @Builder.Default
    private Boolean isPublic = false;

    @Size(max = 64, message = "Checksum must not exceed 64 characters")
    private String checksum;
}
