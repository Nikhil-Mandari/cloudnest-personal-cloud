package com.cloudnest.file.controller;

import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.service.FileService;
import com.cloudnest.file.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for file metadata operations.
 * <p>
 * Provides endpoints for uploading, retrieving, updating, moving, deleting,
 * restoring, and searching file metadata records.
 * <strong>Note:</strong> Actual binary file upload/download is not handled here;
 * this service manages metadata only.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Registers new file metadata after an upload.
     * <p>
     * Accepts metadata fields and persists them as a new file record.
     * A unique file ID (UUID) is generated automatically.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param request      the upload metadata payload
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 201 Created with the newly created file metadata
     */
    @PostMapping("/upload")
    public ResponseEntity<StandardResponse<FileResponse>> uploadFile(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody UploadFileRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/files/upload - userId={}, fileName='{}'",
                userIdHeader, request.getOriginalFileName());

        // Override ownerId with authenticated user from header
        request.setOwnerId(userIdHeader);

        FileResponse response = fileService.uploadFileMetadata(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File uploaded successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Lists all active file metadata records for the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of file metadata records
     */
    @GetMapping
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> listUserFiles(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files - userId={}", userIdHeader);

        List<FileMetadataResponse> files = fileService.getUserFiles(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FileMetadataResponse>>builder()
                        .success(true)
                        .message("Files retrieved successfully")
                        .data(files)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves detailed file metadata by its internal ID.
     *
     * @param id          the internal primary key of the file record
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK with the detailed file metadata
     */
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<FileResponse>> getFileById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/{}", id);

        FileResponse response = fileService.getFileById(id);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File retrieved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Updates select fields of an existing file metadata record.
     * <p>
     * Only the fields provided in the request body are updated.
     *
     * @param id          the internal primary key of the file record
     * @param request     the update payload with optional fields
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK with the updated file metadata
     */
    @PutMapping("/{id}")
    public ResponseEntity<StandardResponse<FileResponse>> updateFileDetails(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFileRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/files/{}", id);

        FileResponse response = fileService.updateFileDetails(id, request);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File updated successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Soft-deletes a file metadata record.
     *
     * @param id          the internal primary key of the file record
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> deleteFile(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/files/{}", id);

        fileService.deleteFile(id);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("File deleted successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Restores a soft-deleted file metadata record.
     *
     * @param id          the internal primary key of the file record
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK with the restored file metadata
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<StandardResponse<FileResponse>> restoreFile(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        log.info("PATCH /api/files/{}/restore", id);

        FileResponse response = fileService.restoreFile(id);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File restored successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Searches for active file records by original file name (case-insensitive).
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param query        the search term (optional — returns all files if empty)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of matching file metadata records
     */
    @GetMapping("/search")
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> searchFiles(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @RequestParam(required = false, defaultValue = "") String query,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/search?query={}", query);

        List<FileMetadataResponse> results = fileService.searchFiles(query, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FileMetadataResponse>>builder()
                        .success(true)
                        .message("Files searched successfully")
                        .data(results)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
