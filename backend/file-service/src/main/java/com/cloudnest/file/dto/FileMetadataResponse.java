package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class FileMetadataResponse {

    private String fileId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private Long ownerId;
    private Long folderId;
    private Boolean isPublic;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
