package com.cloudnest.share.service.impl;

import com.cloudnest.share.client.FileServiceClient;
import com.cloudnest.share.client.FolderServiceClient;
import com.cloudnest.share.client.UserServiceClient;
import com.cloudnest.share.dto.FileResponse;
import com.cloudnest.share.dto.FolderResponse;
import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareWithUserRequest;
import com.cloudnest.share.dto.UpdatePermissionRequest;
import com.cloudnest.share.dto.UserResponse;
import com.cloudnest.share.entity.Share;
import com.cloudnest.share.entity.Share.Permission;
import com.cloudnest.share.entity.Share.ResourceType;
import com.cloudnest.share.exception.DuplicateShareException;
import com.cloudnest.share.exception.ShareExpiredException;
import com.cloudnest.share.exception.ShareNotFoundException;
import com.cloudnest.share.exception.UnauthorizedShareAccessException;
import com.cloudnest.share.mapper.ShareMapper;
import com.cloudnest.share.repository.ShareRepository;
import com.cloudnest.share.service.ShareService;
import com.cloudnest.share.util.StandardResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
                request.getExpiryDate()
        );

        Share saved = shareRepository.save(share);
        log.info("File shared successfully: id={}, fileId={}, sharedWithUserId={}, token={}",
                saved.getId(), fileId, recipientId, saved.getShareToken());

        return shareMapper.toShareResponse(saved);
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
                request.getExpiryDate()
        );

        Share saved = shareRepository.save(share);
        log.info("Folder shared successfully: id={}, folderId={}, sharedWithUserId={}, token={}",
                saved.getId(), folderId, recipientId, saved.getShareToken());

        return shareMapper.toShareResponse(saved);
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
                .map(shareMapper::toShareResponse)
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
                .map(shareMapper::toShareResponse)
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

        return shareMapper.toShareResponse(share);
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
     * @param permission       the permission level (VIEW or EDIT)
     * @param expiryDate       the optional expiry date
     * @return a new Share entity (not yet persisted)
     */
    private Share buildShare(
            String resourceId,
            ResourceType resourceType,
            Long ownerId,
            Long sharedWithUserId,
            Permission permission,
            LocalDateTime expiryDate) {

        return Share.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .ownerId(ownerId)
                .sharedWithUserId(sharedWithUserId)
                .permission(permission)
                .shareToken(UUID.randomUUID().toString())
                .isPublic(false)
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
}
