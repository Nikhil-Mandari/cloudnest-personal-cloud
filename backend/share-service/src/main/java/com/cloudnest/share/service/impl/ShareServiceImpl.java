package com.cloudnest.share.service.impl;

import com.cloudnest.share.client.FileServiceClient;
import com.cloudnest.share.client.FolderServiceClient;
import com.cloudnest.share.client.UserServiceClient;
import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.dto.FolderResponse;
import com.cloudnest.share.dto.ShareAnalyticsResponse;
import com.cloudnest.share.dto.ShareDownloadResponse;
import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareValidationResponse;
import com.cloudnest.share.dto.ShareWithUserRequest;
import com.cloudnest.share.dto.UpdatePermissionRequest;
import com.cloudnest.share.dto.UserResponse;
import com.cloudnest.share.entity.Share;
import com.cloudnest.share.entity.Share.Permission;
import com.cloudnest.share.entity.Share.ResourceType;
import com.cloudnest.share.exception.DuplicateShareException;
import com.cloudnest.share.exception.InvalidSharePasswordException;
import com.cloudnest.share.exception.ShareExpiredException;
import com.cloudnest.share.exception.ShareNotFoundException;
import com.cloudnest.share.exception.SharePasswordRequiredException;
import com.cloudnest.share.exception.UnauthorizedShareAccessException;
import com.cloudnest.share.mapper.ShareMapper;
import com.cloudnest.share.repository.ShareRepository;
import com.cloudnest.share.service.ShareService;
import com.cloudnest.share.util.StandardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link ShareService} interface.
 * <p>
 * Handles all share management operations including sharing files/folders,
 * password-protected public share links, access analytics (view / download
 * counters), public downloads streamed from the File Service, permission
 * updates, revocation, and internal token validation for the File Service.
 * Uses OpenFeign clients to validate resources and users across microservices.
 */
@Slf4j
@Service
@Transactional
public class ShareServiceImpl implements ShareService {

    private final ShareRepository shareRepository;
    private final ShareMapper shareMapper;
    private final UserServiceClient userServiceClient;
    private final FileServiceClient fileServiceClient;
    private final FolderServiceClient folderServiceClient;
    private final PasswordEncoder passwordEncoder;

    public ShareServiceImpl(
            ShareRepository shareRepository,
            ShareMapper shareMapper,
            UserServiceClient userServiceClient,
            FileServiceClient fileServiceClient,
            FolderServiceClient folderServiceClient,
            PasswordEncoder passwordEncoder) {
        this.shareRepository = shareRepository;
        this.shareMapper = shareMapper;
        this.userServiceClient = userServiceClient;
        this.fileServiceClient = fileServiceClient;
        this.folderServiceClient = folderServiceClient;
        this.passwordEncoder = passwordEncoder;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share File
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shares a file with another user.
     * <p>
     * Validates the file exists and belongs to the owner, resolves the recipient
     * user (by ID or email), generates a unique share token, and persists the share.
     */
    @Override
    public ShareResponse shareFile(Long fileId, Long ownerId, ShareWithUserRequest request) {
        log.debug("Sharing file: fileId={}, ownerId={}, sharedWithUserId={}, sharedWithEmail='{}'",
                fileId, ownerId, request.getSharedWithUserId(), request.getSharedWithEmail());

        // ── Validate the file exists ────────────────────────────────────────────
        StandardResponse<FileResponse> fileResponse = fileServiceClient.getFileById(fileId);
        if (fileResponse == null || fileResponse.getData() == null) {
            log.warn("Share file failed: file not found with id={}", fileId);
            throw new ShareNotFoundException("File not found with id: " + fileId);
        }

        FileResponse file = fileResponse.getData();

        // ── Ensure the owner owns the file ──────────────────────────────────────
        if (!file.getOwnerId().equals(ownerId)) {
            log.warn("Share file failed: file {} does not belong to owner {}", fileId, ownerId);
            throw new UnauthorizedShareAccessException("You do not own this file");
        }

        // ── Resolve the recipient user ──────────────────────────────────────────
        Long recipientId = resolveRecipientUserId(request);

        // ── Check for duplicate share ───────────────────────────────────────────
        if (shareRepository.existsByResourceIdAndResourceTypeAndSharedWithUserId(
                String.valueOf(fileId), ResourceType.FILE, recipientId)) {
            log.warn("Share file failed: duplicate share for fileId={}, userId={}",
                    fileId, recipientId);
            throw new DuplicateShareException(
                    "This file is already shared with the specified user");
        }

        // ── Create and persist the share ────────────────────────────────────────
        Share share = buildShare(
                String.valueOf(fileId),
                ResourceType.FILE,
                ownerId,
                recipientId,
                request.getPermission(),
                request.getExpiryDate(),
                request.getPassword()
        );

        Share saved = shareRepository.save(share);
        log.info("File shared successfully: id={}, fileId={}, sharedWithUserId={}, token={}",
                saved.getId(), fileId, recipientId, saved.getShareToken());

        // The file metadata was already fetched above — reuse it for the name.
        ShareResponse response = shareMapper.toShareResponse(saved);
        response.setResourceName(file.getOriginalFileName());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share Folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shares a folder with another user.
     * <p>
     * Validates the folder exists and belongs to the owner, resolves the recipient
     * user (by ID or email), generates a unique share token, and persists the share.
     */
    @Override
    public ShareResponse shareFolder(String folderId, Long ownerId, ShareWithUserRequest request) {
        log.debug("Sharing folder: folderId={}, ownerId={}, sharedWithUserId={}, sharedWithEmail='{}'",
                folderId, ownerId, request.getSharedWithUserId(), request.getSharedWithEmail());

        // ── Validate the folder exists ──────────────────────────────────────────
        StandardResponse<FolderResponse> folderResponse = folderServiceClient.getFolderById(folderId);
        if (folderResponse == null || folderResponse.getData() == null) {
            log.warn("Share folder failed: folder not found with id={}", folderId);
            throw new ShareNotFoundException("Folder not found with id: " + folderId);
        }

        FolderResponse folder = folderResponse.getData();

        // ── Ensure the owner owns the folder ────────────────────────────────────
        // Note: Folder Service uses UUID for ownerId; convert both to String for comparison
        if (!String.valueOf(folder.getOwnerId()).equals(String.valueOf(ownerId))) {
            log.warn("Share folder failed: folder {} does not belong to owner {}", folderId, ownerId);
            throw new UnauthorizedShareAccessException("You do not own this folder");
        }

        // ── Resolve the recipient user ──────────────────────────────────────────
        Long recipientId = resolveRecipientUserId(request);

        // ── Check for duplicate share ───────────────────────────────────────────
        if (shareRepository.existsByResourceIdAndResourceTypeAndSharedWithUserId(
                folderId, ResourceType.FOLDER, recipientId)) {
            log.warn("Share folder failed: duplicate share for folderId={}, userId={}",
                    folderId, recipientId);
            throw new DuplicateShareException(
                    "This folder is already shared with the specified user");
        }

        // ── Create and persist the share ────────────────────────────────────────
        Share share = buildShare(
                folderId,
                ResourceType.FOLDER,
                ownerId,
                recipientId,
                request.getPermission(),
                request.getExpiryDate(),
                request.getPassword()
        );

        Share saved = shareRepository.save(share);
        log.info("Folder shared successfully: id={}, folderId={}, sharedWithUserId={}, token={}",
                saved.getId(), folderId, recipientId, saved.getShareToken());

        // The folder metadata was already fetched above — reuse it for the name.
        ShareResponse response = shareMapper.toShareResponse(saved);
        response.setResourceName(folder.getName());
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get My Shares
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all shares created by the authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ShareResponse> getMyShares(Long ownerId) {
        log.debug("Fetching shares for ownerId={}", ownerId);

        return shareRepository.findByOwnerId(ownerId)
                .stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Shares Shared With Me
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all shares shared with the authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ShareResponse> getSharesSharedWithMe(Long userId) {
        log.debug("Fetching shares shared with userId={}", userId);

        return shareRepository.findBySharedWithUserId(userId)
                .stream()
                .map(this::toEnrichedResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Public Share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a public share by its share token.
     * <p>
     * Validates that the share has not expired before returning the response,
     * and tracks the access as a view for the share's analytics.
     * (Not read-only: the view counter is persisted.)
     */
    @Override
    public ShareResponse getPublicShare(String token) {
        log.debug("Fetching public share by token={}", token);

        Share share = findShareByToken(token);

        // ── Validate the share has not expired ──────────────────────────────────
        validateShareNotExpired(share);

        // ── Track the view ──────────────────────────────────────────────────────
        share.setViewCount((share.getViewCount() == null ? 0L : share.getViewCount()) + 1);
        share.setLastAccessedAt(LocalDateTime.now());
        shareRepository.save(share);

        log.info("Public share accessed: token={}, resourceId={}, resourceType={}",
                token, share.getResourceId(), share.getResourceType());

        return toEnrichedResponse(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update Share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the permission level, expiry date and/or password of an existing
     * share. Only the share owner can update the share.
     */
    @Override
    public ShareResponse updateShare(Long shareId, Long ownerId, UpdatePermissionRequest request) {
        log.debug("Updating share: shareId={}, ownerId={}, newPermission={}",
                shareId, ownerId, request.getPermission());

        Share share = findShareById(shareId);

        // ── Only the owner can update the share ─────────────────────────────────
        if (!share.getOwnerId().equals(ownerId)) {
            log.warn("Update share failed: user {} does not own share {}", ownerId, shareId);
            throw new UnauthorizedShareAccessException("You can only update your own shares");
        }

        // ── Apply updates ───────────────────────────────────────────────────────
        share.setPermission(request.getPermission());

        if (Boolean.TRUE.equals(request.getClearExpiry())) {
            share.setExpiryDate(null);
        } else if (request.getExpiryDate() != null) {
            share.setExpiryDate(request.getExpiryDate());
        }

        // ── Password changes ────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(request.getClearPassword())) {
            share.setPasswordHash(null);
        } else if (request.getPassword() != null && !request.getPassword().isBlank()) {
            share.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        Share saved = shareRepository.save(share);
        log.info("Share updated successfully: shareId={}, newPermission={}",
                shareId, request.getPermission());

        return toEnrichedResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Revoke Share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Revokes (deletes) a share.
     * Only the share owner can revoke the share.
     */
    @Override
    public void revokeShare(Long shareId, Long ownerId) {
        log.debug("Revoking share: shareId={}, ownerId={}", shareId, ownerId);

        Share share = findShareById(shareId);

        // ── Only the owner can revoke the share ─────────────────────────────────
        if (!share.getOwnerId().equals(ownerId)) {
            log.warn("Revoke share failed: user {} does not own share {}", ownerId, shareId);
            throw new UnauthorizedShareAccessException("You can only revoke your own shares");
        }

        shareRepository.delete(share);
        log.info("Share revoked successfully: shareId={}", shareId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify share password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the password of a password-protected public share.
     */
    @Override
    public ShareResponse verifySharePassword(String token, String password) {
        log.debug("Verifying share password: token={}", token);

        Share share = findShareByToken(token);
        validateShareNotExpired(share);
        requirePassword(share, password);

        log.info("Share password verified: token={}, resourceId={}", token, share.getResourceId());
        return toEnrichedResponse(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download public share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams the content of a file shared via a public link.
     * <p>
     * Validates the token, expiry and (when set) password, enforces the share
     * permission (VIEW links are not downloadable), increments the download
     * counter, then streams the bytes from the File Service.
     */
    @Override
    public ShareDownloadResponse downloadPublicShare(String token, String password) {
        log.debug("Downloading public share: token={}", token);

        Share share = findShareByToken(token);
        validateShareNotExpired(share);
        requirePassword(share, password);
        requireDownloadPermission(share);

        ShareDownloadResponse response = streamShareContent(share);

        // ── Track the download (only after the stream actually opened) ─────────
        share.setDownloadCount((share.getDownloadCount() == null ? 0L : share.getDownloadCount()) + 1);
        share.setLastAccessedAt(LocalDateTime.now());
        shareRepository.save(share);

        log.info("Public share downloaded: token={}, fileId={}, size={}",
                token, share.getResourceId(), response.getFileSize());
        return response;
    }

    /**
     * Streams the content of a file shared via a public link for in-browser
     * preview. Never increments the download counter.
     */
    @Override
    public ShareDownloadResponse previewPublicShare(String token, String password) {
        log.debug("Previewing public share: token={}", token);

        Share share = findShareByToken(token);
        validateShareNotExpired(share);
        requirePassword(share, password);

        // Viewing is allowed at every permission level, so only the file-type
        // and stream checks apply here.
        return streamShareContent(share);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Share analytics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns access analytics for one of the owner's shares.
     */
    @Override
    @Transactional(readOnly = true)
    public ShareAnalyticsResponse getShareAnalytics(Long shareId, Long ownerId) {
        log.debug("Fetching analytics: shareId={}, ownerId={}", shareId, ownerId);

        Share share = findShareById(shareId);

        if (!share.getOwnerId().equals(ownerId)) {
            log.warn("Analytics access denied: user {} does not own share {}", ownerId, shareId);
            throw new UnauthorizedShareAccessException(
                    "You can only view analytics for your own shares");
        }

        ShareResponse base = toEnrichedResponse(share);

        return ShareAnalyticsResponse.builder()
                .shareId(share.getId())
                .shareToken(share.getShareToken())
                .resourceId(share.getResourceId())
                .resourceType(share.getResourceType())
                .resourceName(base.getResourceName())
                .permission(share.getPermission())
                .hasPassword(share.getPasswordHash() != null && !share.getPasswordHash().isBlank())
                .isPublic(share.getIsPublic())
                .expiryDate(share.getExpiryDate())
                .createdAt(share.getCreatedAt())
                .viewCount(share.getViewCount())
                .downloadCount(share.getDownloadCount())
                .lastAccessedAt(share.getLastAccessedAt())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal token validation (used by the File Service)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Internal token validation. Never increments counters and never leaks the
     * password hash — the File Service only needs a yes/no answer.
     * <p>
     * Only reachable service-to-service: the controller rejects requests that
     * carry an {@code X-User-Id} header (only the API Gateway adds it), so an
     * authenticated external user cannot use this as a token oracle.
     */
    @Override
    @Transactional(readOnly = true)
    public ShareValidationResponse validateShareToken(String token, String resourceId) {
        ShareValidationResponse.ShareValidationResponseBuilder builder =
                ShareValidationResponse.builder().valid(false);

        try {
            Share share = shareRepository.findByShareToken(token).orElse(null);
            if (share == null) {
                return builder.message("Share not found").build();
            }
            if (share.getExpiryDate() != null && share.getExpiryDate().isBefore(LocalDateTime.now())) {
                return builder.message("Share has expired").build();
            }
            if (resourceId != null && !resourceId.isBlank()
                    && !share.getResourceId().equals(resourceId)) {
                return builder.message("Share does not cover the requested resource").build();
            }
            return builder
                    .valid(true)
                    .resourceId(share.getResourceId())
                    .resourceType(share.getResourceType())
                    .build();
        } catch (Exception e) {
            log.warn("Share token validation failed for token={}: {}", token, e.getMessage());
            return builder.message("Validation failed").build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the recipient user ID from the share request.
     * <p>
     * If a {@code sharedWithUserId} is provided, validates the user exists.
     * If a {@code sharedWithEmail} is provided, looks up the user by email
     * using the User Service search endpoint.
     *
     * @param request the share request containing recipient info
     * @return the resolved recipient user ID
     * @throws IllegalArgumentException if neither userId nor email is provided
     * @throws ShareNotFoundException   if the user cannot be found
     */
    private Long resolveRecipientUserId(ShareWithUserRequest request) {
        if (request.getSharedWithUserId() != null) {
            // ── Validate user exists by ID ──────────────────────────────────────
            try {
                StandardResponse<UserResponse> userResponse =
                        userServiceClient.getUserById(request.getSharedWithUserId());
                if (userResponse == null || userResponse.getData() == null) {
                    log.warn("Recipient user not found: id={}", request.getSharedWithUserId());
                    throw new ShareNotFoundException(
                            "User not found with id: " + request.getSharedWithUserId());
                }
                return userResponse.getData().getId();
            } catch (Exception e) {
                log.warn("Recipient user validation failed: id={}", request.getSharedWithUserId());
                throw new ShareNotFoundException(
                        "User not found with id: " + request.getSharedWithUserId());
            }
        } else if (request.getSharedWithEmail() != null && !request.getSharedWithEmail().isBlank()) {
            // ── Look up user by email via search endpoint ───────────────────────
            try {
                StandardResponse<List<UserResponse>> searchResponse =
                        userServiceClient.searchUsers(request.getSharedWithEmail());
                if (searchResponse == null || searchResponse.getData() == null
                        || searchResponse.getData().isEmpty()) {
                    log.warn("Recipient user not found: email='{}'", request.getSharedWithEmail());
                    throw new ShareNotFoundException(
                            "User not found with email: " + request.getSharedWithEmail());
                }
                // Return the first matching user's ID
                return searchResponse.getData().get(0).getId();
            } catch (Exception e) {
                log.warn("Recipient user lookup failed: email='{}'", request.getSharedWithEmail());
                throw new ShareNotFoundException(
                        "User not found with email: " + request.getSharedWithEmail());
            }
        } else {
            throw new IllegalArgumentException(
                    "Either sharedWithUserId or sharedWithEmail must be provided");
        }
    }

    /**
     * Builds a new {@link Share} entity with a generated UUID share token and
     * an optional BCrypt-hashed password.
     *
     * @param resourceId       the ID of the resource being shared
     * @param resourceType     the type of the resource (FILE or FOLDER)
     * @param ownerId          the ID of the resource owner
     * @param sharedWithUserId the ID of the recipient user
     * @param permission       the permission level (VIEW, DOWNLOAD or EDIT)
     * @param expiryDate       the optional expiry date
     * @param password         the optional share-link password (hashed, never stored raw)
     * @return a new Share entity (not yet persisted)
     */
    private Share buildShare(
            String resourceId,
            ResourceType resourceType,
            Long ownerId,
            Long sharedWithUserId,
            Permission permission,
            LocalDateTime expiryDate,
            String password) {

        Share share = Share.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .ownerId(ownerId)
                .sharedWithUserId(sharedWithUserId)
                .permission(permission)
                .shareToken(UUID.randomUUID().toString())
                .isPublic(false)
                .expiryDate(expiryDate)
                .build();

        if (password != null && !password.isBlank()) {
            share.setPasswordHash(passwordEncoder.encode(password));
        }

        return share;
    }

    /**
     * Maps a {@link Share} entity and enriches the response with the display
     * name of the underlying file/folder.
     */
    private ShareResponse toEnrichedResponse(Share share) {
        return enrichResourceName(shareMapper.toShareResponse(share));
    }

    /**
     * Populates {@code resourceName} on a share response by resolving the
     * underlying file or folder through the Feign clients.
     * <p>
     * Fails open: when the resource cannot be resolved (missing, deleted, or a
     * downstream error), the name is left {@code null} and omitted from the JSON
     * payload thanks to {@code @JsonInclude(NON_NULL)}.
     */
    private ShareResponse enrichResourceName(ShareResponse response) {
        if (response == null || response.getResourceName() != null) {
            return response;
        }
        try {
            if (response.getResourceType() == ResourceType.FILE) {
                StandardResponse<FileResponse> fileResponse =
                        fileServiceClient.getFileById(Long.valueOf(response.getResourceId()));
                if (fileResponse != null && fileResponse.getData() != null) {
                    response.setResourceName(fileResponse.getData().getOriginalFileName());
                }
            } else {
                StandardResponse<FolderResponse> folderResponse =
                        folderServiceClient.getFolderById(response.getResourceId());
                if (folderResponse != null && folderResponse.getData() != null) {
                    response.setResourceName(folderResponse.getData().getName());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve resource name for share id={} resourceId={} type={}",
                    response.getId(), response.getResourceId(), response.getResourceType(), e);
        }
        return response;
    }

    /**
     * Resolves the display name of a shared file for the download response,
     * falling back to a neutral name when the File Service is unreachable.
     */
    private String resolveSharedFileName(Share share) {
        try {
            StandardResponse<FileResponse> fileResponse =
                    fileServiceClient.getFileById(Long.valueOf(share.getResourceId()));
            if (fileResponse != null && fileResponse.getData() != null
                    && fileResponse.getData().getOriginalFileName() != null) {
                return fileResponse.getData().getOriginalFileName();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve shared file name for share id={}: {}",
                    share.getId(), e.getMessage());
        }
        return "shared-file";
    }

    /**
     * Validates that a share has not expired.
     *
     * @param share the share to validate
     * @throws ShareExpiredException if the share has expired
     */
    private void validateShareNotExpired(Share share) {
        if (share.getExpiryDate() != null && share.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Share has expired: shareId={}, expiryDate={}",
                    share.getId(), share.getExpiryDate());
            throw new ShareExpiredException(
                    "This share has expired since " + share.getExpiryDate());
        }
    }

    /**
     * Enforces password protection on a share link.
     *
     * @param share    the share being accessed
     * @param password the submitted password (may be null)
     * @throws SharePasswordRequiredException when the share is protected and no
     *         password is supplied
     * @throws InvalidSharePasswordException  when the supplied password is wrong
     */
    private void requirePassword(Share share, String password) {
        if (share.getPasswordHash() == null || share.getPasswordHash().isBlank()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new SharePasswordRequiredException(
                    "This share is password protected — a password is required");
        }
        if (!passwordEncoder.matches(password, share.getPasswordHash())) {
            throw new InvalidSharePasswordException("The share password is incorrect");
        }
    }

    /**
     * Enforces the share permission for public downloads: links granted
     * {@code VIEW} only may be viewed, not downloaded.
     *
     * @param share the share being downloaded
     * @throws UnauthorizedShareAccessException when the share is view-only
     */
    private void requireDownloadPermission(Share share) {
        if (share.getPermission() == Permission.VIEW) {
            log.warn("Public download blocked: share id={} is view-only", share.getId());
            throw new UnauthorizedShareAccessException(
                    "This link only allows viewing — downloading is disabled");
        }
    }

    /**
     * Opens the underlying file's byte stream through the File Service.
     * Shared by the download and preview flows (no counters touched here).
     *
     * @param share the share whose file content should be streamed
     * @return the streamed content plus metadata
     */
    private ShareDownloadResponse streamShareContent(Share share) {
        if (share.getResourceType() != ResourceType.FILE) {
            throw new IllegalArgumentException("Streaming is only supported for shared files");
        }

        Long fileId = Long.valueOf(share.getResourceId());
        ResponseEntity<Resource> resourceResponse = fileServiceClient.downloadStream(fileId, share.getShareToken());

        Resource resource = resourceResponse.getBody();
        if (resource == null) {
            log.warn("Share stream failed: no content returned for fileId={}", fileId);
            throw new ShareNotFoundException("The shared file content is currently unavailable");
        }

        InputStream stream;
        try {
            stream = resource.getInputStream();
        } catch (IOException e) {
            log.warn("Share stream failed: cannot open stream for fileId={}: {}",
                    fileId, e.getMessage());
            throw new ShareNotFoundException("The shared file content is currently unavailable");
        }

        String contentType = resourceResponse.getHeaders().getContentType() != null
                ? resourceResponse.getHeaders().getContentType().toString()
                : "application/octet-stream";

        return ShareDownloadResponse.builder()
                .inputStream(stream)
                .originalFileName(resolveSharedFileName(share))
                .contentType(contentType)
                .fileSize(resourceResponse.getHeaders().getContentLength())
                .build();
    }

    /**
     * Internal helper to find a share by ID or throw {@link ShareNotFoundException}.
     *
     * @param shareId the share ID
     * @return the found Share entity
     * @throws ShareNotFoundException if no record exists with the given ID
     */
    private Share findShareById(Long shareId) {
        return shareRepository.findById(shareId)
                .orElseThrow(() -> {
                    log.warn("Share not found: shareId={}", shareId);
                    return new ShareNotFoundException("Share not found with id: " + shareId);
                });
    }

    /**
     * Internal helper to find a share by token or throw {@link ShareNotFoundException}.
     *
     * @param token the public share token
     * @return the found Share entity
     * @throws ShareNotFoundException if no record exists with the given token
     */
    private Share findShareByToken(String token) {
        return shareRepository.findByShareToken(token)
                .orElseThrow(() -> {
                    log.warn("Share not found: token={}", token);
                    return new ShareNotFoundException("Share not found with token: " + token);
                });
    }
}
