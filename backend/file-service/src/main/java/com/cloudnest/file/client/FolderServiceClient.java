package com.cloudnest.file.client;

import com.cloudnest.file.dto.FolderResponse;
import com.cloudnest.file.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

/**
 * OpenFeign client for communicating with the Folder Service.
 * <p>
 * Used to validate that folders referenced by file uploads / moves actually
 * exist and belong to the authenticated user. The {@code X-User-Id} header is
 * forwarded so the Folder Service itself enforces folder ownership.
 */
@FeignClient(name = "folder-service", path = "/api/folders")
public interface FolderServiceClient {

    /**
     * Retrieves a folder by its UUID for the given owner.
     * <p>
     * The Folder Service returns 404 when the folder does not exist, is
     * soft-deleted, or belongs to a different owner.
     *
     * @param id      the folder's UUID
     * @param ownerId the authenticated user's ID (forwarded as X-User-Id)
     * @return the standard response containing folder data
     */
    @GetMapping("/{id}")
    StandardResponse<FolderResponse> getFolderById(
            @PathVariable("id") String id,
            @RequestHeader("X-User-Id") Long ownerId);

    /**
     * Retrieves every non-deleted folder owned by the user.
     * <p>
     * Used by the storage analytics overview to count folders (including
     * empty ones), which the file service does not own.
     *
     * @param ownerId the authenticated user's ID (forwarded as X-User-Id)
     * @return the standard response containing the user's folders
     */
    @GetMapping
    StandardResponse<List<FolderResponse>> getAllFolders(
            @RequestHeader("X-User-Id") Long ownerId);
}
