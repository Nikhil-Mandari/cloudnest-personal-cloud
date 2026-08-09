package com.cloudnest.file.controller;

import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.service.FileService;
import com.cloudnest.file.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for file operations.
 * <p>
 * Binary content is stored in MinIO object storage while metadata lives in
 * MySQL. All endpoints expect the authenticated user's ID in the
 * {@code X-User-Id} header, which is set by the API Gateway after JWT
 * authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "Upload, download, preview, and manage files. " +
        "Binary content is stored in MinIO; metadata (including SHA-256 checksum) lives in MySQL.")
public class FileController {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Uploads a file (multipart/form-data).
     * <p>
     * The binary content is uploaded to MinIO with a unique
     * {@code UUID_originalFileName} object key; the metadata record — including
     * the SHA-256 checksum — is persisted in MySQL.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param file         the multipart file to upload
     * @param folderId     the destination folder UUID (optional, null = root)
     * @param isPublic     whether the file should be publicly accessible
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 201 Created with the newly created file metadata
     */
    @Operation(
            summary = "Upload a file",
            description = "Uploads a file to MinIO object storage and persists its metadata " +
                    "(object key, bucket, content type, size, SHA-256 checksum) in MySQL.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Empty file, invalid request, or folder not found"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "413", description = "File exceeds the maximum allowed size"),
            @ApiResponse(responseCode = "500", description = "MinIO or database failure")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StandardResponse<FileResponse>> uploadFile(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "The file to upload", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "Destination folder UUID (null uploads to the root)",
                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @RequestParam(required = false) String folderId,
            @Parameter(description = "Whether the file should be publicly accessible", example = "false")
            @RequestParam(defaultValue = "false") boolean isPublic,
            HttpServletRequest httpRequest) {

        log.info("POST /api/files/upload - userId={}, folderId={}, fileName='{}', size={}",
                userIdHeader, folderId, file.getOriginalFilename(), file.getSize());

        UploadFileRequest request = UploadFileRequest.builder()
                .originalFileName(Optional.ofNullable(file.getOriginalFilename()).orElse("file"))
                .contentType(resolveContentType(file.getContentType()))
                .fileSize(file.getSize())
                .ownerId(userIdHeader)
                .folderId(folderId)
                .isPublic(isPublic)
                .build();

        FileResponse response = fileService.uploadFile(request, file);

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
     */
    @Operation(summary = "List user files",
            description = "Returns all active file metadata records owned by the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> listUserFiles(
            @Parameter(hidden = true)
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
     * Searches for active file records by original file name (case-insensitive).
     */
    @Operation(summary = "Search files",
            description = "Searches the authenticated user's active files by original file name (case-insensitive). " +
                    "An empty query returns all files.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files searched successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/search")
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> searchFiles(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "Search term (optional — returns all files when empty)", example = "report")
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

    /**
     * Lists all active file metadata records marked as favorite by the user.
     */
    @Operation(summary = "List favorite files",
            description = "Returns all active file metadata records owned by the authenticated user " +
                    "that were marked as favorite.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorite files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/favorites")
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> listFavoriteFiles(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/favorites - userId={}", userIdHeader);

        List<FileMetadataResponse> files = fileService.getFavoriteFiles(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FileMetadataResponse>>builder()
                        .success(true)
                        .message("Favorite files retrieved successfully")
                        .data(files)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Lists all soft-deleted (trashed) file metadata records for the
     * authenticated user.
     */
    @Operation(summary = "List trashed files",
            description = "Returns all soft-deleted file metadata records owned by the authenticated user " +
                    "(files that were moved to the trash).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trashed files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/trash")
    public ResponseEntity<StandardResponse<List<FileMetadataResponse>>> listTrashFiles(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/trash - userId={}", userIdHeader);

        List<FileMetadataResponse> files = fileService.getTrashFiles(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FileMetadataResponse>>builder()
                        .success(true)
                        .message("Trashed files retrieved successfully")
                        .data(files)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Marks (or unmarks) a file as favorite.
     */
    @Operation(summary = "Mark / unmark a file as favorite",
            description = "Sets the favorite flag on a file. When the `favorite` query parameter is " +
                    "omitted, the current value is toggled.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Favorite updated successfully"),
            @ApiResponse(responseCode = "400", description = "File has been deleted"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PatchMapping("/{id}/favorite")
    public ResponseEntity<StandardResponse<FileResponse>> setFavorite(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "Target favorite state (omitted = toggle)", example = "true")
            @RequestParam(required = false) Boolean favorite,
            HttpServletRequest httpRequest) {

        log.info("PATCH /api/files/{}/favorite - userId={}, favorite={}", id, userIdHeader, favorite);

        FileResponse response = fileService.setFavorite(id, favorite, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("Favorite updated successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves detailed file metadata by its internal ID.
     */
    @Operation(summary = "Get file metadata",
            description = "Returns detailed metadata for a single file. Ownership is enforced " +
                    "for authenticated callers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<FileResponse>> getFileById(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/{}", id);

        FileResponse response = fileService.getFileById(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File retrieved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Streams a file's binary content from MinIO for download.
     * <p>
     * Sets {@code Content-Type} and {@code Content-Disposition: attachment}
     * so every file type can be downloaded.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @return the file content as a binary resource
     */
    @Operation(summary = "Download a file",
            description = "Streams the file's binary content from MinIO with " +
                    "Content-Disposition: attachment. Supports every file type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File content streamed successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "File has been deleted"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File or its content not found"),
            @ApiResponse(responseCode = "500", description = "MinIO failure")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader) {

        log.info("GET /api/files/{}/download - userId={}", id, userIdHeader);

        FileDownloadResponse download = fileService.downloadFile(id, userIdHeader);

        return ResponseEntity.ok()
                .contentType(parseContentType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        buildContentDisposition(download.getOriginalFileName(), false))
                .body(new InputStreamResource(download.getInputStream()));
    }

    /**
     * Streams a file's binary content from MinIO for in-browser preview.
     * <p>
     * Uses {@code Content-Disposition: inline}. Supports pdf, png, jpg, jpeg,
     * gif, and txt.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @return the file content as an inline binary resource
     */
    @Operation(summary = "Preview a file",
            description = "Streams the file's binary content inline for browser preview. " +
                    "Supports: pdf, png, jpg, jpeg, gif, txt.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File content streamed inline",
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", description = "Preview not supported for this file type"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File or its content not found"),
            @ApiResponse(responseCode = "500", description = "MinIO failure")
    })
    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previewFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader) {

        log.info("GET /api/files/{}/preview - userId={}", id, userIdHeader);

        FileDownloadResponse preview = fileService.previewFile(id, userIdHeader);

        return ResponseEntity.ok()
                .contentType(parseContentType(preview.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        buildContentDisposition(preview.getOriginalFileName(), true))
                .body(new InputStreamResource(preview.getInputStream()));
    }

    /**
     * Updates select fields of an existing file metadata record (rename).
     * <p>
     * Renames metadata only — the MinIO object key is never renamed.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @param request      the update payload with optional fields
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated file metadata
     */
    @Operation(summary = "Rename / update file metadata",
            description = "Updates select metadata fields (original file name, file type, visibility). " +
                    "The MinIO object key is never renamed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File updated successfully"),
            @ApiResponse(responseCode = "400", description = "File has been deleted or invalid payload"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<StandardResponse<FileResponse>> updateFileDetails(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody UpdateFileRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/files/{} - userId={}", id, userIdHeader);

        FileResponse response = fileService.updateFileDetails(id, request, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File updated successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Moves a file to a different folder.
     * <p>
     * Only the {@code folderId} is updated — the MinIO object is never moved.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @param folderId     the destination folder UUID (null moves the file to root)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated file metadata
     */
    @Operation(summary = "Move a file to another folder",
            description = "Updates the file's folderId only. The MinIO object is never moved.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File moved successfully"),
            @ApiResponse(responseCode = "400", description = "Folder not found or file has been deleted"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PatchMapping("/{id}/move")
    public ResponseEntity<StandardResponse<FileResponse>> moveFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "Destination folder UUID (null moves to root)",
                    example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
            @RequestParam(required = false) String folderId,
            HttpServletRequest httpRequest) {

        log.info("PATCH /api/files/{}/move - userId={}, folderId={}", id, userIdHeader, folderId);

        FileResponse response = fileService.moveFile(id, folderId, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File moved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Hard-deletes a file: removes the object from MinIO and deletes the
     * metadata row from MySQL.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @Operation(summary = "Delete a file",
            description = "Permanently deletes a file: removes the object from MinIO and deletes the " +
                    "metadata row from MySQL. If the MinIO deletion fails, nothing is deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "MinIO failure")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> deleteFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/files/{} - userId={}", id, userIdHeader);

        fileService.deleteFile(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("File deleted successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Restores a soft-deleted (legacy) file record by setting its status back
     * to {@code ACTIVE}.
     *
     * @param id           the internal primary key of the file record
     * @param userIdHeader the authenticated user's ID
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the restored file metadata
     */
    @Operation(summary = "Restore a soft-deleted file (legacy)",
            description = "Restores a legacy soft-deleted file record by setting its status back to ACTIVE. " +
                    "Files deleted through the DELETE endpoint are permanently removed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File restored successfully"),
            @ApiResponse(responseCode = "400", description = "File is already active"),
            @ApiResponse(responseCode = "403", description = "File belongs to another user"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<StandardResponse<FileResponse>> restoreFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("PATCH /api/files/{}/restore - userId={}", id, userIdHeader);

        FileResponse response = fileService.restoreFile(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("File restored successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the content type, falling back to a default when absent.
     */
    private String resolveContentType(String contentType) {
        return (contentType == null || contentType.isBlank())
                ? DEFAULT_CONTENT_TYPE
                : contentType;
    }

    /**
     * Parses a MIME type safely.
     */
    private MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(resolveContentType(contentType));
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Builds an RFC 5987-compliant Content-Disposition header value.
     *
     * @param fileName the file name to embed
     * @param inline   {@code true} for inline (preview), {@code false} for attachment (download)
     * @return the header value
     */
    private String buildContentDisposition(String fileName, boolean inline) {
        String safeName = (fileName == null || fileName.isBlank())
                ? "file"
                : fileName.replaceAll("[\\r\\n\"]", "_");
        String encoded = URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = inline ? "inline" : "attachment";
        return disposition + "; filename=\"" + safeName + "\"; filename*=UTF-8''" + encoded;
    }
}
