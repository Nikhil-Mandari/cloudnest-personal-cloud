package com.cloudnest.folder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response payload containing all folder properties.
 * <p>
 * Returned for all folder operations including creation, retrieval,
 * rename, move, and listing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderResponse {

    private UUID id;
    private String name;
    private Long ownerId;
    private UUID parentFolderId;
    private String path;
    private Integer level;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
