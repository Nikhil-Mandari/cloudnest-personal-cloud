package com.cloudnest.share.service;

import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareWithUserRequest;
import com.cloudnest.share.dto.UpdatePermissionRequest;

import java.util.List;

/**
 * Service interface for share management operations.
 * <p>
 * Defines the contract for sharing files and folders with other users,
 * managing public share tokens, updating permissions, revoking shares,
 * and retrieving shared resources.
 */
public interface ShareService {

    /**
     * Shares a file with another user.
     *
     * @param fileId  the ID of the file to share
     * @param ownerId the ID of the file owner
     * @param request the share request containing recipient info and permission
     * @return the created share response
     */
    ShareResponse shareFile(Long fileId, Long ownerId, ShareWithUserRequest request);

    /**
     * Shares a folder with another user.
     *
     * @param folderId the ID of the folder to share
     * @param ownerId  the ID of the folder owner
     * @param request  the share request containing recipient info and permission
     * @return the created share response
     */
    ShareResponse shareFolder(String folderId, Long ownerId, ShareWithUserRequest request);

    /**
     * Retrieves all shares created by the authenticated user.
     *
     * @param ownerId the ID of the share owner
     * @return a list of share responses
     */
    List<ShareResponse> getMyShares(Long ownerId);

    /**
     * Retrieves all shares shared with the authenticated user.
     *
     * @param userId the ID of the recipient user
     * @return a list of share responses
     */
    List<ShareResponse> getSharesSharedWithMe(Long userId);

    /**
     * Retrieves a public share by its share token.
     *
     * @param token the public share token
     * @return the share response if valid and not expired
     */
    ShareResponse getPublicShare(String token);

    /**
     * Updates the permission level and/or expiry date of an existing share.
     *
     * @param shareId     the ID of the share to update
     * @param ownerId     the ID of the share owner
     * @param request     the update payload with new permission and/or expiry
     * @return the updated share response
     */
    ShareResponse updateShare(Long shareId, Long ownerId, UpdatePermissionRequest request);

    /**
     * Revokes (deletes) a share.
     *
     * @param shareId the ID of the share to revoke
     * @param ownerId the ID of the share owner
     */
    void revokeShare(Long shareId, Long ownerId);
}
