package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for a bulk ZIP download.
 * <p>
 * At least one of {@code fileIds} or {@code folderIds} must be non-empty; the
 * service validates ownership of every referenced resource and preserves the
 * folder hierarchy inside the archive.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bulk ZIP download request")
public class DownloadZipRequest {

    @Schema(description = "Internal ids of the files to include", example = "[1, 2]")
    @Size(max = 1000, message = "Too many files requested")
    private List<Long> fileIds;

    @Schema(description = "Folder UUIDs to include (recursively, hierarchy preserved)", example = "[\"7c9e6679-…\"]")
    @Size(max = 100, message = "Too many folders requested")
    private List<String> folderIds;

    /** Normalises null lists to empty lists. */
    public List<Long> safeFileIds() {
        return fileIds != null ? fileIds : new ArrayList<>();
    }

    /** Normalises null lists to empty lists. */
    public List<String> safeFolderIds() {
        return folderIds != null ? folderIds : new ArrayList<>();
    }
}
