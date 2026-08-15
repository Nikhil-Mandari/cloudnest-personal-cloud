package com.cloudnest.share.repository;

import com.cloudnest.share.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Share} entity operations.
 * <p>
 * Provides standard CRUD plus custom queries for share management features
 * such as finding shares by owner, recipient, resource, token, and public shares.
 */
@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    /**
     * Finds all shares created by a specific owner.
     *
     * @param ownerId the ID of the share owner
     * @return a list of shares owned by the specified user
     */
    List<Share> findByOwnerId(Long ownerId);

    /**
     * Finds all shares shared with a specific user.
     *
     * @param sharedWithUserId the ID of the recipient user
     * @return a list of shares shared with the specified user
     */
    List<Share> findBySharedWithUserId(Long sharedWithUserId);

    /**
     * Finds a share by its unique share token.
     *
     * @param shareToken the unique share token
     * @return an {@link Optional} containing the matching share, or empty if not found
     */
    Optional<Share> findByShareToken(String shareToken);

    /**
     * Finds all shares for a specific resource.
     *
     * @param resourceId   the ID of the resource
     * @param resourceType the type of the resource (FILE or FOLDER)
     * @return a list of shares for the specified resource
     */
    List<Share> findByResourceIdAndResourceType(String resourceId, Share.ResourceType resourceType);

    /**
     * Finds a share by resource, recipient, and type (to check for duplicates).
     *
     * @param resourceId       the ID of the resource
     * @param resourceType     the type of the resource
     * @param sharedWithUserId the ID of the recipient user
     * @return an {@link Optional} containing the matching share, or empty if not found
     */
    Optional<Share> findByResourceIdAndResourceTypeAndSharedWithUserId(
            String resourceId, Share.ResourceType resourceType, Long sharedWithUserId);

    /**
     * Finds all public shares for a specific resource.
     *
     * @param resourceId   the ID of the resource
     * @param resourceType the type of the resource
     * @return a list of public shares for the specified resource
     */
    List<Share> findByResourceIdAndResourceTypeAndIsPublicTrue(
            String resourceId, Share.ResourceType resourceType);

    /**
     * Checks whether a share exists for a given resource and recipient.
     *
     * @param resourceId       the ID of the resource
     * @param resourceType     the type of the resource
     * @param sharedWithUserId the ID of the recipient user
     * @return {@code true} if a share exists
     */
    boolean existsByResourceIdAndResourceTypeAndSharedWithUserId(
            String resourceId, Share.ResourceType resourceType, Long sharedWithUserId);
}
