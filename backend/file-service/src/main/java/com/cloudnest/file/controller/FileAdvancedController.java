package com.cloudnest.file.controller;

import com.cloudnest.file.dto.DownloadZipRequest;
import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.FileVersionResponse;
import com.cloudnest.file.dto.PagedAuditLogsResponse;
import com.cloudnest.file.dto.ScanStatusResponse;
import com.cloudnest.file.dto.StorageOverviewResponse;
import com.cloudnest.file.service.FileService;
import com.cloudnest.file.service.VersionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST controller for the advanced file-management features: version history,
 * bulk ZIP download, storage analytics, audit trail and virus-scan status.
 * <p>
 * All endpoints expect the authenticated user's ID in the {@code X-User-Id}
 * header (set by the API Gateway after JWT authentication).
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management — Advanced",
        description = "Version history, bulk ZIP download, storage analytics, audit trail and virus-scan status.")
public class FileAdvancedController {

    private final VersionService versionService;
    private final FileService fileService;

    public FileAdvancedController(VersionService versionService, FileService fileService) {
        this.versionService = versionService;
        this.fileService = fileService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Version history
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "List file versions",
            description = "Lists the archived content snapshots of a file, newest first. "
                    + "The current content is the file itself and is not part of this list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Versions retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "File has been deleted"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}/versions")
    public ResponseEntity<StandardResponse<List<FileVersionResponse>>> listVersions(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/{}/versions - userId={}", id, userIdHeader);

        List<FileVersionResponse> versions = versionService.getVersions(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FileVersionResponse>>builder()
                        .success(true)
                        .message("Versions retrieved successfully")
                        .data(versions)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "Upload a new version",
            description = "Replaces the file's content: the current content is archived as a "
                    + "version and the uploaded file becomes the current version.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New version uploaded"),
            @ApiResponse(responseCode = "400", description = "Empty file or file has been deleted"),
            @ApiResponse(responseCode = "413", description = "File exceeds the maximum allowed size"),
            @ApiResponse(responseCode = "422", description = "Virus detected — version blocked"),
            @ApiResponse(responseCode = "500", description = "MinIO or database failure")
    })
    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StandardResponse<FileResponse>> uploadNewVersion(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "The new version content", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")))
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        log.info("POST /api/files/{}/versions - userId={}, size={}", id, userIdHeader, file.getSize());

        FileResponse response = versionService.uploadNewVersion(id, file, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("New version uploaded")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "Restore a version",
            description = "Restores an archived version as the file's current content. The "
                    + "current content is archived first, so nothing is lost.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version restored"),
            @ApiResponse(responseCode = "400", description = "File has been deleted"),
            @ApiResponse(responseCode = "404", description = "File or version not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @PostMapping("/{id}/versions/{versionId}/restore")
    public ResponseEntity<StandardResponse<FileResponse>> restoreVersion(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Version record ID", example = "10")
            @PathVariable Long versionId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("POST /api/files/{}/versions/{}/restore - userId={}", id, versionId, userIdHeader);

        FileResponse response = versionService.restoreVersion(id, versionId, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FileResponse>builder()
                        .success(true)
                        .message("Version restored")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "Delete a version",
            description = "Deletes an archived version and its content from MinIO. The version "
                    + "holding the file's current content cannot be deleted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version deleted"),
            @ApiResponse(responseCode = "400", description = "Version is the current content"),
            @ApiResponse(responseCode = "404", description = "File or version not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @DeleteMapping("/{id}/versions/{versionId}")
    public ResponseEntity<StandardResponse<Void>> deleteVersion(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Version record ID", example = "10")
            @PathVariable Long versionId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/files/{}/versions/{} - userId={}", id, versionId, userIdHeader);

        versionService.deleteVersion(id, versionId, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Version deleted")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "Download a version",
            description = "Streams an archived version's binary content.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Version content streamed"),
            @ApiResponse(responseCode = "400", description = "File has been deleted"),
            @ApiResponse(responseCode = "404", description = "File or version not found"),
            @ApiResponse(responseCode = "500", description = "MinIO failure")
    })
    @GetMapping("/{id}/versions/{versionId}/download")
    public ResponseEntity<Resource> downloadVersion(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Version record ID", example = "10")
            @PathVariable Long versionId,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader) {

        log.info("GET /api/files/{}/versions/{}/download - userId={}", id, versionId, userIdHeader);

        FileDownloadResponse download = versionService.downloadVersion(id, versionId, userIdHeader);

        return ResponseEntity.ok()
                .contentType(parseContentType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        buildContentDisposition(download.getOriginalFileName(), false))
                .body(new InputStreamResource(download.getInputStream()));
    }

    @Operation(summary = "Stream a shared file",
            description = "Streams a file's content for a public share download. The share "
                    + "token is validated against the Share Service (expiry and password "
                    + "enforced there) and must cover this resource — no owner context is "
                    + "required. Called by the Share Service over service-to-service Feign.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File content streamed"),
            @ApiResponse(responseCode = "400", description = "File has been deleted or scan in progress"),
            @ApiResponse(responseCode = "403", description = "Invalid or mismatched share token"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "MinIO or Share Service failure")
    })
    @GetMapping("/{id}/share-stream")
    public ResponseEntity<Resource> streamSharedFile(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(description = "Validated public share token", example = "550e8400-…")
            @RequestParam("token") String token) {

        log.info("GET /api/files/{}/share-stream - token={}", id, token);

        FileDownloadResponse download = fileService.downloadSharedFile(id, token);

        return ResponseEntity.ok()
                .contentType(parseContentType(download.getContentType()))
                .contentLength(download.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        buildContentDisposition(download.getOriginalFileName(), false))
                .body(new InputStreamResource(download.getInputStream()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bulk ZIP download
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Download files as ZIP",
            description = "Streams a ZIP archive of the selected files and folders. The folder "
                    + "hierarchy is preserved under a CloudNest/ root directory.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ZIP archive streamed",
                    content = @Content(mediaType = "application/zip")),
            @ApiResponse(responseCode = "400", description = "Empty selection or too many files"),
            @ApiResponse(responseCode = "404", description = "A selected file or folder was not found"),
            @ApiResponse(responseCode = "500", description = "MinIO failure")
    })
    @PostMapping("/download-zip")
    public ResponseEntity<StreamingResponseBody> downloadZip(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody DownloadZipRequest request) {

        log.info("POST /api/files/download-zip - userId={}, files={}, folders={}",
                userIdHeader, request.safeFileIds().size(), request.safeFolderIds().size());

        StreamingResponseBody body = outputStream ->
                fileService.downloadZip(request, outputStream, userIdHeader);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + URLEncoder.encode("cloudnest-export.zip",
                                StandardCharsets.UTF_8) + "\"")
                .body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Storage analytics
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Storage analytics overview",
            description = "Returns the user's storage totals, trash usage, largest files, "
                    + "file-type breakdown and weekly / monthly usage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Overview computed"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/stats/overview")
    public ResponseEntity<StandardResponse<StorageOverviewResponse>> storageOverview(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/stats/overview - userId={}", userIdHeader);

        StorageOverviewResponse overview = fileService.getStorageOverview(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<StorageOverviewResponse>builder()
                        .success(true)
                        .message("Storage overview retrieved successfully")
                        .data(overview)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audit trail
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Audit trail",
            description = "Returns a paged list of the user's file-activity audit entries "
                    + "(uploads, downloads, renames, deletes, restores, versions, …).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Audit entries retrieved"),
            @ApiResponse(responseCode = "401", description = "Missing user identity"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/audit-logs")
    public ResponseEntity<StandardResponse<PagedAuditLogsResponse>> auditLogs(
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional action filter (e.g. UPLOAD)", example = "UPLOAD")
            @RequestParam(required = false) String action,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/audit-logs - userId={}, page={}, size={}, action={}",
                userIdHeader, page, size, action);

        PagedAuditLogsResponse logs = fileService.getAuditLogs(userIdHeader, page, size, action);

        return ResponseEntity.ok(
                StandardResponse.<PagedAuditLogsResponse>builder()
                        .success(true)
                        .message("Audit entries retrieved successfully")
                        .data(logs)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Virus-scan status
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Virus-scan status",
            description = "Returns the current virus-scan status of a file.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved"),
            @ApiResponse(responseCode = "404", description = "File not found"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/{id}/scan-status")
    public ResponseEntity<StandardResponse<ScanStatusResponse>> scanStatus(
            @Parameter(description = "Internal file ID", example = "1")
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/files/{}/scan-status - userId={}", id, userIdHeader);

        ScanStatusResponse status = fileService.getScanStatus(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<ScanStatusResponse>builder()
                        .success(true)
                        .message("Scan status retrieved successfully")
                        .data(status)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers (mirror FileController)
    // ─────────────────────────────────────────────────────────────────────────

    private MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(
                    contentType == null || contentType.isBlank()
                            ? "application/octet-stream" : contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String buildContentDisposition(String fileName, boolean inline) {
        String disposition = inline ? "inline" : "attachment";
        String encoded = URLEncoder.encode(fileName == null ? "file" : fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return disposition + "; filename*=UTF-8''" + encoded;
    }
}
