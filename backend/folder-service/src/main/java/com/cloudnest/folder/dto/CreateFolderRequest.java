package com.cloudnest.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for creating a new folder.
 * <p>
 * The {@code ownerId} is never taken from the request body — it is extracted
 * from the authenticated JWT and forwarded by the API Gateway via a header.
 * The {@code parentFolderId} is optional; if {@code null}, the folder is
 * created as a root-level folder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateFolderRequest {

    @NotBlank(message = "Folder name must not be blank")
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    private String name;

    /**
     * ID of the parent folder (nullable for root folders).
     */
    private UUID parentFolderId;
}
