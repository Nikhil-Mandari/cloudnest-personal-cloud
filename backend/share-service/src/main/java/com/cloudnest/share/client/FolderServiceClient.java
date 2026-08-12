package com.cloudnest.share.client;

import com.cloudnest.share.dto.FolderResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * OpenFeign client for communicating with the Folder Service.
 * <p>
 * Used to validate that folders being shared exist and to retrieve
 * folder metadata for share operations.
 * <p>
 * The {@code X-User-Id} header is always sent with the <em>resource owner's</em>
 * ID so the Folder Service's ownership checks still run on every request.
 */
@FeignClient(name = "folder-service", path = "/api/folders")
public interface FolderServiceClient {

    /**
     * Retrieves a folder by its UUID.
     *
     * @param id      the folder's UUID
     * @param ownerId the folder owner's user ID (resource owner for shared access)
     * @return the standard response containing folder data
     */
    @GetMapping("/{id}")
    StandardResponse<FolderResponse> getFolderById(
            @PathVariable("id") String id,
            @RequestHeader("X-User-Id") Long ownerId);
}
