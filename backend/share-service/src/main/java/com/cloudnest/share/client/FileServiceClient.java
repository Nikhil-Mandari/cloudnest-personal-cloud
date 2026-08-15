package com.cloudnest.share.client;

import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * OpenFeign client for communicating with the File Service.
 * <p>
 * Used to validate that files being shared exist and to retrieve
 * file metadata for share operations.
 * <p>
 * The {@code X-User-Id} header is always sent with the <em>resource owner's</em>
 * ID so the File Service's ownership checks still run on every request. The
 * owner always owns the shared resource, so these checks pass for legitimate
 * share-mediated access while rejecting any direct unauthenticated request.
 */
@FeignClient(name = "file-service", path = "/api/files")
public interface FileServiceClient {

    /**
     * Retrieves a file by its internal ID.
     *
     * @param id      the file's internal ID
     * @param ownerId the file owner's user ID (resource owner for shared access)
     * @return the standard response containing file data
     */
    @GetMapping("/{id}")
    StandardResponse<FileResponse> getFileById(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long ownerId);

    /**
     * Streams the raw file content for download.
     *
     * @param id      the file's internal ID
     * @param ownerId the file owner's user ID (resource owner for shared access)
     * @return the raw file bytes
     */
    @GetMapping("/{id}/download")
    byte[] downloadFileContent(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long ownerId);

    /**
     * Streams the raw file content for in-browser preview.
     *
     * @param id      the file's internal ID
     * @param ownerId the file owner's user ID (resource owner for shared access)
     * @return the raw file bytes
     */
    @GetMapping("/{id}/preview")
    byte[] previewFileContent(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long ownerId);
}
