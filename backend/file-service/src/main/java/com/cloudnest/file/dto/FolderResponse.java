package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal folder representation returned by the Folder Service.
 * <p>
 * Used only for deserialising Feign responses when validating that a folder
 * exists before placing a file into it.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Folder metadata received from the Folder Service (via Feign)")
public class FolderResponse {

    @Schema(description = "Folder UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    private UUID id;

    @Schema(description = "Folder display name", example = "Documents")
    private String name;

    @Schema(description = "ID of the user who owns the folder", example = "1")
    private Long ownerId;

    @Schema(description = "Parent folder UUID (null for root folders)", example = "null")
    private UUID parentFolderId;

    @Schema(description = "Full hierarchical path", example = "/Documents")
    private String path;

    @Schema(description = "Whether the folder is soft-deleted", example = "false")
    private Boolean deleted;
}
