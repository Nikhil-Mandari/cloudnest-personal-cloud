package com.cloudnest.share.service.impl;

import com.cloudnest.share.client.FileServiceClient;
import com.cloudnest.share.client.FolderServiceClient;
import com.cloudnest.share.client.UserServiceClient;
import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.dto.FolderResponse;
import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareWithUserRequest;
import com.cloudnest.share.dto.SharedFileContent;
import com.cloudnest.share.dto.UpdatePermissionRequest;
import com.cloudnest.share.dto.UserResponse;
import com.cloudnest.share.entity.Share;
import com.cloudnest.share.entity.Share.Permission;
import com.cloudnest.share.entity.Share.ResourceType;
import com.cloudnest.share.exception.DuplicateShareException;
import com.cloudnest.share.exception.ShareExpiredException;
import com.cloudnest.share.exception.ShareNotFoundException;
import com.cloudnest.share.exception.SharePasswordException;
import com.cloudnest.share.exception.UnauthorizedShareAccessException;
import com.cloudnest.share.mapper.ShareMapper;
import com.cloudnest.share.repository.ShareRepository;
import com.cloudnest.share.service.ShareService;
import com.cloudnest.share.util.StandardResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link ShareService} interface.
 * <p>
 * Handles all share management operations including sharing files/folders,
 * managing public share tokens, updating permissions, revoking shares,
 * and retrieving shared resources. Uses OpenFeign clients to validate
 * resources and users across microservices.
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

    public ShareServiceImpl(
            ShareRepository shareRepository,
            ShareMapper shareMapper,
            UserServiceClient userServiceClient,
            FileServiceClient fileServiceClient,
            FolderServiceClient folderServiceClient) {
        this.shareRepository = shareRepository;
        this.shareMapper = shareMapper;
        this.userServiceClient = userServiceClient;
        this.fileServiceClient = fileServiceClient;
        this.folderServiceClient = folderServiceClient;
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
        // Pass the owner's ID so the File Service's ownership check still runs;
        // translate the owning service's Feign errors back into share-domain
        // exceptions so 403/404 semantics survive the service boundary.
        FileResponse file;
        try {
            StandardResponse<FileResponse> fileResponse =
                    fileServiceClient.getFileById(fileId, ownerId);
            if (fileResponse == null || fileResponse.getData() == null) {
                log.warn("Share file failed: file not found with id={}", fileId);
                throw new ShareNotFoundException("File not found with id: " + fileId);
            }
            file = fileResponse.getData();
        } catch (ShareNotFoundException e) {
            throw e;
        } catch (FeignException.Forbidden | FeignException.Unauthorized e) {
            log.warn("Share file failed: forbidden file access id={}: {}", fileId, e.getMessage());
            throw new UnauthorizedShareAccessException("You do not own this file");
        } catch (FeignException e) {
            log.warn("Share file failed: file service error for id={}: {}", fileId, e.getMessage());
            throw new ShareNotFoundException("File not found with id: " + fileId);
        }

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
        log.info("File shared successfully: id={}, fileId={}, sharedWithUserId={}, token={}, hasPassword={}",
                saved.getId(), fileId, recipientId, saved.getShareToken(), saved.getPasswordHash() != null);

        return enrichWithResourceName(shareMapper.toShareResponse(saved), ResourceType.FILE, saved.getOwnerId());
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
        // Pass the owner's ID so the Folder Service's ownership check still runs;
        // translate the owning service's Feign errors back into share-domain
        // exceptions so 404 semantics survive the service boundary.
        FolderResponse folder;
        try {
            StandardResponse<FolderResponse> folderResponse =
                    folderServiceClient.getFolderById(folderId, ownerId);
            if (folderResponse == null || folderResponse.getData() == null) {
                log.warn("Share folder failed: folder not found with id={}", folderId);
                throw new ShareNotFoundException("Folder not found with id: " + folderId);
            }
            folder = folderResponse.getData();
        } catch (ShareNotFoundException e) {
            throw e;
        } catch (FeignException e) {
            log.warn("Share folder failed: folder service error for id={}: {}", folderId, e.getMessage());
            throw new ShareNotFoundException("Folder not found with id: " + folderId);
        }

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
        log.info("Folder shared successfully: id={}, folderId={}, sharedWithUserId={}, token={}, hasPassword={}",
                saved.getId(), folderId, recipientId, saved.getShareToken(), saved.getPasswordHash() != null);

        return enrichWithResourceName(shareMapper.toShareResponse(saved), ResourceType.FOLDER, saved.getOwnerId());
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
                .map(share -> enrichWithResourceName(
                        shareMapper.toShareResponse(share), share.getResourceType(), share.getOwnerId()))
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
                .map(share -> enrichWithResourceName(
                        shareMapper.toShareResponse(share), share.getResourceType(), share.getOwnerId()))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Public Share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a public share by its share token.
     * <p>
     * Validates that the share has not expired before returning the response.
     */
    @Override
    @Transactional(readOnly = true)
    public ShareResponse getPublicShare(String token) {
        log.debug("Fetching public share by token={}", token);

        Share share = shareRepository.findByShareToken(token)
                .orElseThrow(() -> {
                    log.warn("Public share not found: token={}", token);
                    return new ShareNotFoundException("Share not found with token: " + token);
                });

        // ── Validate the share has not expired ──────────────────────────────────
        validateShareNotExpired(share);

        log.info("Public share accessed: token={}, resourceId={}, resourceType={}",
                token, share.getResourceId(), share.getResourceType());

        return enrichWithResourceName(
                shareMapper.toShareResponse(share), share.getResourceType(), share.getOwnerId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Verify Share Password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the password of a password-protected public share link.
     */
    @Override
    @Transactional(readOnly = true)
    public ShareResponse verifySharePassword(String token, String password) {
        Share share = findShareByToken(token);
        validateShareNotExpired(share);
        requireCorrectPassword(share, password);

        log.info("Share password verified: token={}, resourceId={}", token, share.getResourceId());
        return enrichWithResourceName(
                shareMapper.toShareResponse(share), share.getResourceType(), share.getOwnerId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public Download / Preview
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams a shared file's content for download through a public link.
     */
    @Override
    @Transactional(readOnly = true)
    public SharedFileContent downloadPublicShare(String token, String password) {
        Share share = requireShareableFile(token, password);

        // VIEW shares are preview-only — downloading is reserved for
        // DOWNLOAD / EDIT permissions (matches the frontend share dialog).
        if (share.getPermission() == Permission.VIEW) {
            log.warn("Download denied for view-only share: token={}", token);
            throw new UnauthorizedShareAccessException(
                    "This share is view-only — downloading is disabled");
        }

        return fetchSharedContent(share, false);
    }

    /**
     * Streams a shared file's content for in-browser preview through a public
     * link. Every permission level may preview.
     */
    @Override
    @Transactional(readOnly = true)
    public SharedFileContent previewPublicShare(String token, String password) {
        Share share = requireShareableFile(token, password);
        return fetchSharedContent(share, true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public-access helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves a FILE share by token and enforces expiry + password.
     */
    private Share requireShareableFile(String token, String password) {
        Share share = findShareByToken(token);
        validateShareNotExpired(share);
        requireCorrectPassword(share, password);

        if (share.getResourceType() != ResourceType.FILE) {
            throw new ShareNotFoundException("Share not found with token: " + token);
        }
        return share;
    }

    /**
     * Fetches the raw file content from the File Service and builds the
     * response payload. Preview uses the preview endpoint (which rejects
     * non-previewable types), download uses the download endpoint.
     */
    private SharedFileContent fetchSharedContent(Share share, boolean preview) {
        Long fileId = Long.valueOf(share.getResourceId());
        byte[] content;
        try {
            content = preview
                    ? fileServiceClient.previewFileContent(fileId, share.getOwnerId())
                    : fileServiceClient.downloadFileContent(fileId, share.getOwnerId());
        } catch (Exception e) {
            log.warn("Failed to fetch shared content: token={}, fileId={}, preview={}: {}",
                    share.getShareToken(), fileId, preview, e.getMessage());
            throw new ShareNotFoundException("The shared file is no longer available");
        }

        StandardResponse<FileResponse> metadata =
                fileServiceClient.getFileById(fileId, share.getOwnerId());
        FileResponse file = metadata != null ? metadata.getData() : null;

        return SharedFileContent.builder()
                .originalFileName(file != null ? file.getOriginalFileName() : "shared-file")
                .contentType(file != null && file.getFileType() != null
                        ? file.getFileType()
                        : "application/octet-stream")
                .fileSize(content != null ? (long) content.length : 0L)
                .content(content != null ? content : new byte[0])
                .build();
    }

    /**
     * Enforces the share-link password when one is set.
     */
    private void requireCorrectPassword(Share share, String password) {
        if (share.getPasswordHash() == null || share.getPasswordHash().isBlank()) {
            return;
        }
        if (password == null || password.isBlank()) {
            throw new SharePasswordException("This link is password protected");
        }
        if (!hashPassword(password).equals(share.getPasswordHash())) {
            throw new SharePasswordException("Incorrect share password");
        }
    }

    /**
     * Best-effort resolution of the shared resource's display name so the
     * frontend can show "shared file.pdf" instead of a blank label.
     * <p>
     * The resource owner's ID is forwarded so the owning service's ownership
     * checks still run on the internal metadata lookups.
     */
    private ShareResponse enrichWithResourceName(
            ShareResponse response, ResourceType type, Long ownerId) {
        if (response.getResourceName() != null) {
            return response;
        }
        try {
            if (type == ResourceType.FILE) {
                StandardResponse<FileResponse> file = fileServiceClient.getFileById(
                        Long.valueOf(response.getResourceId()), ownerId);
                if (file != null && file.getData() != null) {
                    response.setResourceName(file.getData().getOriginalFileName());
                }
            } else {
                StandardResponse<FolderResponse> folder =
                        folderServiceClient.getFolderById(response.getResourceId(), ownerId);
                if (folder != null && folder.getData() != null) {
                    response.setResourceName(folder.getData().getName());
                }
            }
        } catch (Exception e) {
            log.debug("Could not resolve resource name for share id={}: {}",
                    response.getId(), e.getMessage());
        }
        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update Share
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates the permission level and/or expiry date of an existing share.
     * Only the share owner can update the share.
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

        if (request.getExpiryDate() != null) {
            share.setExpiryDate(request.getExpiryDate());
        }

        Share saved = shareRepository.save(share);
        log.info("Share updated successfully: shareId={}, newPermission={}",
                shareId, request.getPermission());

        return shareMapper.toShareResponse(saved);
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
     * Builds a new {@link Share} entity with a generated UUID share token.
     *
     * @param resourceId       the ID of the resource being shared
     * @param resourceType     the type of the resource (FILE or FOLDER)
     * @param ownerId          the ID of the resource owner
     * @param sharedWithUserId the ID of the recipient user
     * @param permission       the permission level (VIEW / DOWNLOAD / EDIT)
     * @param expiryDate       the optional expiry date
     * @param password         the optional plain-text link password (hashed)
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

        return Share.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .ownerId(ownerId)
                .sharedWithUserId(sharedWithUserId)
                .permission(permission)
                .shareToken(UUID.randomUUID().toString())
                .isPublic(false)
                .passwordHash(password == null || password.isBlank() ? null : hashPassword(password))
                .expiryDate(expiryDate)
                .build();
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
     * Internal helper to find a share by its public token or throw
     * {@link ShareNotFoundException}.
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

    /**
     * SHA-256 hash of a share-link password (salted with the token-less
     * constant salt is unnecessary here — the password itself is high-entropy
     * user input and the hash is only used for equality checks).
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /** Random hex helper kept for symmetric generation (reserved for future use). */
    @SuppressWarnings("unused")
    private String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
