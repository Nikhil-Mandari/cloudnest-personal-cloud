package com.cloudnest.folder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for moving a folder to a different parent folder.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoveFolderRequest {

    @NotNull(message = "Destination folder ID must not be null")
    private UUID destinationFolderId;
}
