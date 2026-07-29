package com.cloudnest.file.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating file metadata fields.
 * <p>
 * All fields are optional — only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFileRequest {

    @Size(max = 255, message = "Original file name must not exceed 255 characters")
    private String originalFileName;

    @Size(max = 100, message = "File type must not exceed 100 characters")
    private String fileType;

    private Boolean isPublic;
}
