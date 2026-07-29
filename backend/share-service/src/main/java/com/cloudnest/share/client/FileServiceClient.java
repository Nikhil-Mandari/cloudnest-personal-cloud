package com.cloudnest.share.client;

import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for communicating with the File Service.
 * <p>
 * Used to validate that files being shared exist and to retrieve
 * file metadata for share operations.
 */
@FeignClient(name = "file-service", path = "/api/files")
public interface FileServiceClient {

    /**
     * Retrieves a file by its internal ID.
     *
     * @param id the file's internal ID
     * @return the standard response containing file data
     */
    @GetMapping("/{id}")
    StandardResponse<FileResponse> getFileById(@PathVariable("id") Long id);
}
