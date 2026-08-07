package com.cloudnest.share.service;

import com.cloudnest.share.dto.ShareAnalyticsResponse;
import com.cloudnest.share.dto.ShareDownloadResponse;
import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.dto.ShareValidationResponse;
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

    /**
     * Verifies the password of a password-protected public share.
     *
     * @param token    the public share token
     * @param password the submitted password
     * @return the share response when the password is correct
     * @throws com.cloudnest.share.exception.SharePasswordRequiredException if the
     *         share is not password protected or no password is supplied
     * @throws com.cloudnest.share.exception.InvalidSharePasswordException if the
     *         password is incorrect
     */
    ShareResponse verifySharePassword(String token, String password);

    /**
     * Streams the content of a file shared via a public link.
     * <p>
     * Validates the token, expiry and (when set) password, enforces the
     * share permission (VIEW links are not downloadable), increments the
     * download counter, then streams the bytes from the File Service.
     *
     * @param token    the public share token
     * @param password the submitted password (null when the link is open)
     * @return the streamed file content plus metadata
     */
    ShareDownloadResponse downloadPublicShare(String token, String password);

    /**
     * Streams the content of a file shared via a public link for in-browser
     * preview.
     * <p>
     * Identical to {@link #downloadPublicShare(String, String)} except that it
     * never increments the download counter (only the view counter recorded by
     * {@link #getPublicShare(String)} applies), keeping owner analytics honest.
     *
     * @param token    the public share token
     * @param password the submitted password (null when the link is open)
     * @return the streamed file content plus metadata
     */
    ShareDownloadResponse previewPublicShare(String token, String password);

    /**
     * Returns access analytics for one of the owner's shares.
     *
     * @param shareId the ID of the share
     * @param ownerId the ID of the share owner
     * @return the analytics snapshot
     */
    ShareAnalyticsResponse getShareAnalytics(Long shareId, Long ownerId);

    /**
     * Internal token validation used by the File Service to authorize
     * share-link downloads. Does not increment any counters.
     *
     * @param token      the public share token
     * @param resourceId the resource ID the token must cover (nullable)
     * @return the validation result
     */
    ShareValidationResponse validateShareToken(String token, String resourceId);
}
