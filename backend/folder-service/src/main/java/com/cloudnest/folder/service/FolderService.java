package com.cloudnest.folder.service;

import com.cloudnest.folder.dto.CreateFolderRequest;
import com.cloudnest.folder.dto.FolderResponse;
import com.cloudnest.folder.dto.MoveFolderRequest;
import com.cloudnest.folder.dto.UpdateFolderRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for folder management operations.
 * <p>
 * Defines the contract for managing hierarchical folder structures including
 * CRUD, recursive soft-delete, move with cycle prevention, and nested path updates.
 */
public interface FolderService {

    /**
     * Creates a new folder under the specified parent (or as a root folder).
     * <p>
     * Generates the folder path automatically and validates:
     * <ul>
     *   <li>Folder name is not empty</li>
     *   <li>No duplicate names exist in the same parent</li>
     *   <li>Parent folder exists (if specified)</li>
     * </ul>
     *
     * @param ownerId the authenticated user's UUID (from JWT, never from request body)
     * @param request the creation payload (name + optional parentFolderId)
     * @return the created folder's response
     */
    FolderResponse createFolder(Long ownerId, CreateFolderRequest request);

    /**
     * Renames a folder and recursively updates all child folder paths.
     *
     * @param folderId the UUID of the folder to rename
     * @param ownerId  the authenticated user's UUID
     * @param request  the rename payload containing the new name
     * @return the updated folder's response
     */
    FolderResponse renameFolder(UUID folderId, Long ownerId, UpdateFolderRequest request);

    /**
     * Soft-deletes a folder and recursively soft-deletes all child folders.
     *
     * @param folderId the UUID of the folder to delete
     * @param ownerId  the authenticated user's UUID
     */
    void deleteFolder(UUID folderId, Long ownerId);

    /**
     * Moves a folder to a different parent folder.
     * <p>
     * Prevents:
     * <ul>
     *   <li>Moving a folder into itself</li>
     *   <li>Moving a parent folder into one of its descendants</li>
     *   <li>Duplicate folder names in the destination</li>
     * </ul>
     * Recursively updates all child folder paths after the move.
     *
     * @param folderId the UUID of the folder to move
     * @param ownerId  the authenticated user's UUID
     * @param request  the move payload containing the destination folder ID
     * @return the updated folder's response
     */
    FolderResponse moveFolder(UUID folderId, Long ownerId, MoveFolderRequest request);

    /**
     * Retrieves a single folder by its UUID.
     *
     * @param folderId the UUID of the folder
     * @param ownerId  the authenticated user's UUID
     * @return the folder's response
     */
    FolderResponse getFolder(UUID folderId, Long ownerId);

    /**
     * Retrieves all non-deleted folders for the authenticated user.
     *
     * @param ownerId the authenticated user's UUID
     * @return a list of folder responses
     */
    List<FolderResponse> getAllFolders(Long ownerId);

    /**
     * Retrieves all root-level folders (parentFolderId is null) for the authenticated user.
     *
     * @param ownerId the authenticated user's UUID
     * @return a list of root folder responses
     */
    List<FolderResponse> getRootFolders(Long ownerId);

    /**
     * Retrieves the immediate children of a specific folder.
     *
     * @param parentFolderId the UUID of the parent folder
     * @param ownerId        the authenticated user's UUID
     * @return a list of child folder responses
     */
    List<FolderResponse> getFolderChildren(UUID parentFolderId, Long ownerId);
}
