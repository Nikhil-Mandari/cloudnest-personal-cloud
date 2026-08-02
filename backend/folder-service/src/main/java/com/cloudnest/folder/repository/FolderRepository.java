package com.cloudnest.folder.repository;

import com.cloudnest.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Folder} entity operations.
 * <p>
 * Provides standard CRUD plus custom queries for hierarchical folder management
 * including soft-delete filtering, path-based lookups, and ownership scoping.
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    /**
     * Checks whether a folder with the given name exists under a specific parent
     * for a given owner, excluding soft-deleted records.
     *
     * @param ownerId        the owner's UUID
     * @param parentFolderId the parent folder's UUID (may be null for root-level check)
     * @param name           the folder name to check
     * @return {@code true} if a non-deleted folder with that name exists at that location
     */
    boolean existsByOwnerIdAndParentFolderIdAndNameAndDeletedFalse(
            Long ownerId, UUID parentFolderId, String name);

    /**
     * Finds all folders belonging to a specific owner (including deleted).
     *
     * @param ownerId the owner's UUID
     * @return a list of folders owned by the specified user
     */
    List<Folder> findByOwnerId(Long ownerId);

    /**
     * Finds all non-deleted folders belonging to a specific owner.
     *
     * @param ownerId the owner's UUID
     * @return a list of active folders owned by the specified user
     */
    List<Folder> findByOwnerIdAndDeletedFalse(Long ownerId);

    /**
     * Finds all folders with a specific parent folder ID (including deleted).
     *
     * @param parentFolderId the parent folder's UUID
     * @return a list of folders under the specified parent
     */
    List<Folder> findByParentFolderId(UUID parentFolderId);

    /**
     * Finds all non-deleted folders with a specific parent folder ID.
     *
     * @param parentFolderId the parent folder's UUID
     * @return a list of active folders under the specified parent
     */
    List<Folder> findByParentFolderIdAndDeletedFalse(UUID parentFolderId);

    /**
     * Finds all folders owned by a user under a specific parent (including deleted).
     *
     * @param ownerId        the owner's UUID
     * @param parentFolderId the parent folder's UUID
     * @return a list of folders matching the owner and parent
     */
    List<Folder> findByOwnerIdAndParentFolderId(Long ownerId, UUID parentFolderId);

    /**
     * Finds all non-deleted folders owned by a user under a specific parent.
     *
     * @param ownerId        the owner's UUID
     * @param parentFolderId the parent folder's UUID
     * @return a list of active folders matching the owner and parent
     */
    List<Folder> findByOwnerIdAndParentFolderIdAndDeletedFalse(Long ownerId, UUID parentFolderId);

    /**
     * Finds a folder by its exact hierarchical path.
     *
     * @param path the folder path (e.g. "/Documents/Java")
     * @return an {@link Optional} containing the matching folder, or empty if not found
     */
    Optional<Folder> findByPath(String path);

    /**
     * Finds a folder by its exact hierarchical path within a specific owner's scope.
     *
     * @param ownerId the owner's UUID
     * @param path    the folder path (e.g. "/Documents/Java")
     * @return an {@link Optional} containing the matching folder, or empty if not found
     */
    Optional<Folder> findByOwnerIdAndPath(Long ownerId, String path);

    /**
     * Finds all folders whose path starts with the given prefix.
     * Used for recursive path updates when renaming or moving folders.
     *
     * @param pathPrefix the path prefix to match (e.g. "/Documents/Java%")
     * @return a list of folders with paths starting with the given prefix
     */
    List<Folder> findByPathStartingWith(String pathPrefix);
}
