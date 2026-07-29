package com.cloudnest.share.client;

import com.cloudnest.share.dto.FolderResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for communicating with the Folder Service.
 * <p>
 * Used to validate that folders being shared exist and to retrieve
 * folder metadata for share operations.
 */
@FeignClient(name = "folder-service", path = "/api/folders")
public interface FolderServiceClient {

    /**
     * Retrieves a folder by its UUID.
     *
     * @param id the folder's UUID
     * @return the standard response containing folder data
     */
    @GetMapping("/{id}")
    StandardResponse<FolderResponse> getFolderById(@PathVariable("id") String id);
}
