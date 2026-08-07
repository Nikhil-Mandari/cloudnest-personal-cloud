package com.cloudnest.share.client;

import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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

    /**
     * Streams a shared file's content.
     * <p>
     * The File Service validates the share token against the Share Service
     * before streaming — the token is the capability for this download.
     *
     * @param id    the file's internal ID
     * @param token the validated public share token
     * @return the streamed binary content
     */
    @GetMapping("/{id}/share-stream")
    ResponseEntity<Resource> downloadStream(@PathVariable("id") Long id,
                                            @RequestParam("token") String token);
}
