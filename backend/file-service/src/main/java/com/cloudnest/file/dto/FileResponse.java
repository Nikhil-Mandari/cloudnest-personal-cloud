package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class FileResponse {

    private Long id;
    private String fileId;
    private String originalFileName;
    private String storedFileName;
    private String fileType;
    private Long fileSize;
    private String storagePath;
    private Long ownerId;
    private Long folderId;
    private Boolean isPublic;
    private String checksum;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
