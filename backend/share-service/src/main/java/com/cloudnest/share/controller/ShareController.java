package com.cloudnest.share.controller;

import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareWithUserRequest;
import com.cloudnest.share.dto.SharedFileContent;
import com.cloudnest.share.dto.UpdatePermissionRequest;
import com.cloudnest.share.service.ShareService;
import com.cloudnest.share.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for share management operations.
 * <p>
 * Provides endpoints for sharing files and folders with other users,
 * managing public share tokens, updating permissions, revoking shares,
 * and retrieving shared resources.
 * The authenticated user's ID is received via the {@code X-User-Id} header,
 * which is set by the API Gateway after JWT authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Shares a file with another user.
     *
     * @param fileId       the internal ID of the file to share
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param request      the share request containing recipient info and permission
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 201 Created with the newly created share
     */
    @PostMapping("/file/{fileId}")
    public ResponseEntity<StandardResponse<ShareResponse>> shareFile(
            @PathVariable Long fileId,
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody ShareWithUserRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/shares/file/{} - userId={}, sharedWithUserId={}, sharedWithEmail='{}'",
                fileId, userIdHeader, request.getSharedWithUserId(), request.getSharedWithEmail());

        ShareResponse response = shareService.shareFile(fileId, userIdHeader, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<ShareResponse>builder()
                        .success(true)
                        .message("File shared successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Shares a folder with another user.
     *
     * @param folderId     the UUID (as string) of the folder to share
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param request      the share request containing recipient info and permission
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 201 Created with the newly created share
     */
    @PostMapping("/folder/{folderId}")
    public ResponseEntity<StandardResponse<ShareResponse>> shareFolder(
            @PathVariable String folderId,
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody ShareWithUserRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/shares/folder/{} - userId={}, sharedWithUserId={}, sharedWithEmail='{}'",
                folderId, userIdHeader, request.getSharedWithUserId(), request.getSharedWithEmail());

        ShareResponse response = shareService.shareFolder(folderId, userIdHeader, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<ShareResponse>builder()
                        .success(true)
                        .message("Folder shared successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves all shares created by the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of share responses
     */
    @GetMapping("/my-shares")
    public ResponseEntity<StandardResponse<List<ShareResponse>>> getMyShares(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/shares/my-shares - userId={}", userIdHeader);

        List<ShareResponse> shares = shareService.getMyShares(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<ShareResponse>>builder()
                        .success(true)
                        .message("Shares retrieved successfully")
                        .data(shares)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves all shares shared with the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of share responses
     */
    @GetMapping("/shared-with-me")
    public ResponseEntity<StandardResponse<List<ShareResponse>>> getSharesSharedWithMe(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/shares/shared-with-me - userId={}", userIdHeader);

        List<ShareResponse> shares = shareService.getSharesSharedWithMe(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<ShareResponse>>builder()
                        .success(true)
                        .message("Shared-with-me shares retrieved successfully")
                        .data(shares)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves a public share by its share token.
     * <p>
     * This endpoint does not require authentication, allowing public access
     * to shared resources via a unique token.
     *
     * @param token       the public share token (UUID)
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK with the share details if valid and not expired
     */
    @GetMapping("/public/{token}")
    public ResponseEntity<StandardResponse<ShareResponse>> getPublicShare(
            @PathVariable String token,
            HttpServletRequest httpRequest) {

        log.info("GET /api/shares/public/{}", token);

        ShareResponse response = shareService.getPublicShare(token);

        return ResponseEntity.ok(
                StandardResponse.<ShareResponse>builder()
                        .success(true)
                        .message("Public share retrieved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Verifies the password of a password-protected public share link.
     * <p>
     * Does not require authentication (the share token is the credential).
     *
     * @param token       the public share token
     * @param password    the plain-text password supplied by the visitor
     * @param httpRequest the current HTTP request (for building response path)
     * @return 200 OK with the share details once the password is verified
     */
    @PostMapping("/public/{token}/verify-password")
    public ResponseEntity<StandardResponse<ShareResponse>> verifySharePassword(
            @PathVariable String token,
            @RequestBody(required = false) com.cloudnest.share.dto.VerifySharePasswordRequest body,
            HttpServletRequest httpRequest) {

        log.info("POST /api/shares/public/{}/verify-password", token);

        String password = body != null ? body.getPassword() : null;
        ShareResponse response = shareService.verifySharePassword(token, password);

        return ResponseEntity.ok(
                StandardResponse.<ShareResponse>builder()
                        .success(true)
                        .message("Share password verified successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Streams the shared file's content for download through a public link.
     * <p>
     * Does not require authentication; the share token (and, when set, the
     * link password via the {@code X-Share-Password} header) gates access.
     * VIEW-only shares reject downloads.
     *
     * @param token    the public share token
     * @param password the link password (required when the link is protected)
     * @return the file content as an attachment
     */
    @GetMapping("/public/{token}/download")
    public ResponseEntity<Resource> downloadPublicShare(
            @PathVariable String token,
            @RequestHeader(value = "X-Share-Password", required = false) String password) {

        log.info("GET /api/shares/public/{}/download", token);

        SharedFileContent content = shareService.downloadPublicShare(token, password);

        return ResponseEntity.ok()
                .contentType(parseContentType(content.getContentType()))
                .contentLength(content.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFileName(content.getOriginalFileName()) + "\"")
                .body(new ByteArrayResource(content.getContent()));
    }

    /**
     * Streams the shared file's content for in-browser preview through a
     * public link. Does not require authentication.
     *
     * @param token    the public share token
     * @param password the link password (required when the link is protected)
     * @return the file content inline
     */
    @GetMapping("/public/{token}/preview")
    public ResponseEntity<Resource> previewPublicShare(
            @PathVariable String token,
            @RequestHeader(value = "X-Share-Password", required = false) String password) {

        log.info("GET /api/shares/public/{}/preview", token);

        SharedFileContent content = shareService.previewPublicShare(token, password);

        return ResponseEntity.ok()
                .contentType(parseContentType(content.getContentType()))
                .contentLength(content.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + safeFileName(content.getOriginalFileName()) + "\"")
                .body(new ByteArrayResource(content.getContent()));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private MediaType parseContentType(String contentType) {
        try {
            return MediaType.parseMediaType(
                    contentType == null || contentType.isBlank()
                            ? "application/octet-stream"
                            : contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String safeFileName(String fileName) {
        return (fileName == null || fileName.isBlank())
                ? "shared-file"
                : fileName.replaceAll("[\\r\\n\"]", "_");
    }

    /**
     * Updates the permission level and/or expiry date of an existing share.
     * Only the share owner can update the share.
     *
     * @param shareId      the ID of the share to update
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param request      the update payload with new permission and/or expiry
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated share
     */
    @PutMapping("/{shareId}")
    public ResponseEntity<StandardResponse<ShareResponse>> updateShare(
            @PathVariable Long shareId,
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody UpdatePermissionRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/shares/{} - userId={}, permission={}",
                shareId, userIdHeader, request.getPermission());

        ShareResponse response = shareService.updateShare(shareId, userIdHeader, request);

        return ResponseEntity.ok(
                StandardResponse.<ShareResponse>builder()
                        .success(true)
                        .message("Share updated successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Revokes (deletes) a share.
     * Only the share owner can revoke the share.
     *
     * @param shareId      the ID of the share to revoke
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the revocation
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<StandardResponse<Void>> revokeShare(
            @PathVariable Long shareId,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/shares/{} - userId={}", shareId, userIdHeader);

        shareService.revokeShare(shareId, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Share revoked successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
