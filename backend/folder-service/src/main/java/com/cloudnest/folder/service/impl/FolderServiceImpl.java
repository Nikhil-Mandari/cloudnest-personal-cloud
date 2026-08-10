package com.cloudnest.folder.service.impl;

import com.cloudnest.folder.dto.CreateFolderRequest;
import com.cloudnest.folder.dto.FolderResponse;
import com.cloudnest.folder.dto.MoveFolderRequest;
import com.cloudnest.folder.dto.UpdateFolderRequest;
import com.cloudnest.folder.entity.Folder;
import com.cloudnest.folder.exception.DuplicateFolderException;
import com.cloudnest.folder.exception.FolderNotFoundException;
import com.cloudnest.folder.exception.InvalidFolderOperationException;
import com.cloudnest.folder.mapper.FolderMapper;
import com.cloudnest.folder.repository.FolderRepository;
import com.cloudnest.folder.service.FolderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link FolderService} interface.
 * <p>
 * Handles all folder management operations including hierarchical CRUD,
 * recursive soft-delete, move with cycle detection, and automatic path
 * recalculation for nested folder trees.
 */
@Slf4j
@Service
@Transactional
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;
    private final FolderMapper folderMapper;

    public FolderServiceImpl(FolderRepository folderRepository, FolderMapper folderMapper) {
        this.folderRepository = folderRepository;
        this.folderMapper = folderMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new folder under the specified parent (or as a root folder).
     * <p>
     * Validates:
     * <ul>
     *   <li>Duplicate names are not allowed inside the same parent</li>
     *   <li>Parent folder must exist if specified</li>
     * </ul>
     * Generates the folder path and level automatically.
     */
    @Override
    public FolderResponse createFolder(Long ownerId, CreateFolderRequest request) {
        log.debug("Creating folder: name='{}', ownerId={}, parentFolderId={}",
                request.getName(), ownerId, request.getParentFolderId());

        // ── Validate duplicate name in the same parent ──────────────────────────
        if (folderRepository.existsByOwnerIdAndParentFolderIdAndNameAndDeletedFalse(
                ownerId, request.getParentFolderId(), request.getName())) {
            log.warn("Create folder failed: duplicate name '{}' in parent {}",
                    request.getName(), request.getParentFolderId());
            throw new DuplicateFolderException(
                    "A folder with the name '" + request.getName() + "' already exists in this location");
        }

        // ── Resolve parent folder (if specified) ────────────────────────────────
        String parentPath = "";
        int level = 0;

        if (request.getParentFolderId() != null) {
            Folder parentFolder = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> {
                        log.warn("Create folder failed: parent folder not found: id={}",
                                request.getParentFolderId());
                        return new FolderNotFoundException(
                                "Parent folder not found with id: " + request.getParentFolderId());
                    });

            // Ensure parent belongs to the same owner
            if (!parentFolder.getOwnerId().equals(ownerId)) {
                log.warn("Create folder failed: parent folder {} does not belong to owner {}",
                        request.getParentFolderId(), ownerId);
                throw new FolderNotFoundException(
                        "Parent folder not found with id: " + request.getParentFolderId());
            }

            parentPath = parentFolder.getPath();
            level = parentFolder.getLevel() + 1;
        }

        // ── Build and persist the folder entity ─────────────────────────────────
        String folderPath = parentPath + "/" + request.getName();

        Folder folder = Folder.builder()
                .name(request.getName())
                .ownerId(ownerId)
                .parentFolderId(request.getParentFolderId())
                .path(folderPath)
                .level(level)
                .deleted(false)
                .build();

        Folder saved = folderRepository.save(folder);
        log.info("Folder created successfully: id={}, name='{}', path='{}', level={}",
                saved.getId(), saved.getName(), saved.getPath(), saved.getLevel());

        return folderMapper.toFolderResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rename Folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Renames a folder and recursively updates all child folder paths.
     * <p>
     * The old path prefix is replaced with the new path prefix throughout
     * the entire subtree.
     */
    @Override
    public FolderResponse renameFolder(UUID folderId, Long ownerId, UpdateFolderRequest request) {
        log.debug("Renaming folder: id={}, newName='{}'", folderId, request.getName());

        Folder folder = findActiveFolderByIdAndOwner(folderId, ownerId);

        // ── Check for duplicate name at the same level ──────────────────────────
        if (folderRepository.existsByOwnerIdAndParentFolderIdAndNameAndDeletedFalse(
                ownerId, folder.getParentFolderId(), request.getName())) {
            log.warn("Rename failed: duplicate name '{}' in parent {}",
                    request.getName(), folder.getParentFolderId());
            throw new DuplicateFolderException(
                    "A folder with the name '" + request.getName() + "' already exists in this location");
        }

        String oldPath = folder.getPath();
        String newPath = computeNewPath(folder, request.getName());

        // ── Update the folder itself ────────────────────────────────────────────
        folder.setName(request.getName());
        folder.setPath(newPath);
        folderRepository.save(folder);

        // ── Recursively update all child paths ───────────────────────────────────
        updateChildPathsRecursively(oldPath, newPath);

        log.info("Folder renamed successfully: id={}, oldPath='{}', newPath='{}'",
                folderId, oldPath, newPath);

        return folderMapper.toFolderResponse(folder);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Folder (Soft Delete — Recursive)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a folder and recursively soft-deletes all child folders.
     */
    @Override
    public void deleteFolder(UUID folderId, Long ownerId) {
        log.debug("Soft-deleting folder: id={}", folderId);

        Folder folder = findActiveFolderByIdAndOwner(folderId, ownerId);

        // ── Recursively soft-delete the folder and all descendants ──────────────
        softDeleteRecursively(folder);

        log.info("Folder soft-deleted successfully: id={}, path='{}'", folderId, folder.getPath());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Move Folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Moves a folder to a different parent folder.
     * <p>
     * Prevents:
     * <ul>
     *   <li>Moving into itself (same ID)</li>
     *   <li>Moving a parent into one of its descendants (cycle)</li>
     *   <li>Duplicate folder names at the destination</li>
     * </ul>
     * Recursively updates all child folder paths after the move.
     */
    @Override
    public FolderResponse moveFolder(UUID folderId, Long ownerId, MoveFolderRequest request) {
        log.debug("Moving folder: id={}, destinationFolderId={}",
                folderId, request.getDestinationFolderId());

        Folder folder = findActiveFolderByIdAndOwner(folderId, ownerId);
        UUID destinationId = request.getDestinationFolderId();

        // ── Prevent moving into itself ──────────────────────────────────────────
        if (folderId.equals(destinationId)) {
            log.warn("Move failed: cannot move folder into itself: id={}", folderId);
            throw new InvalidFolderOperationException("Cannot move a folder into itself");
        }

        // ── Resolve destination folder ─────────────────────────────────────────
        Folder destination = folderRepository.findById(destinationId)
                .orElseThrow(() -> {
                    log.warn("Move failed: destination folder not found: id={}", destinationId);
                    return new FolderNotFoundException(
                            "Destination folder not found with id: " + destinationId);
                });

        // Ensure destination belongs to the same owner
        if (!destination.getOwnerId().equals(ownerId)) {
            log.warn("Move failed: destination folder {} does not belong to owner {}",
                    destinationId, ownerId);
            throw new FolderNotFoundException(
                    "Destination folder not found with id: " + destinationId);
        }

        // ── Prevent moving parent into one of its descendants (cycle prevention) ─
        if (isDescendant(destinationId, folder)) {
            log.warn("Move failed: cannot move folder {} into its descendant {}",
                    folderId, destinationId);
            throw new InvalidFolderOperationException(
                    "Cannot move a folder into one of its own descendants");
        }

        // ── Check for duplicate name at the destination ──────────────────────────
        if (folderRepository.existsByOwnerIdAndParentFolderIdAndNameAndDeletedFalse(
                ownerId, destinationId, folder.getName())) {
            log.warn("Move failed: duplicate name '{}' in destination folder {}",
                    folder.getName(), destinationId);
            throw new DuplicateFolderException(
                    "A folder with the name '" + folder.getName() + "' already exists in the destination");
        }

        String oldPath = folder.getPath();
        String newPath = destination.getPath() + "/" + folder.getName();

        // ── Update the folder itself ────────────────────────────────────────────
        folder.setParentFolderId(destinationId);
        folder.setPath(newPath);
        folder.setLevel(destination.getLevel() + 1);
        folderRepository.save(folder);

        // ── Recursively update all child paths ───────────────────────────────────
        updateChildPathsRecursively(oldPath, newPath);

        log.info("Folder moved successfully: id={}, oldPath='{}', newPath='{}'",
                folderId, oldPath, newPath);

        return folderMapper.toFolderResponse(folder);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Folder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a single folder by its UUID, ensuring it belongs to the owner.
     */
    @Override
    @Transactional(readOnly = true)
    public FolderResponse getFolder(UUID folderId, Long ownerId) {
        log.debug("Fetching folder: id={}, ownerId={}", folderId, ownerId);

        Folder folder = findActiveFolderByIdAndOwner(folderId, ownerId);
        return folderMapper.toFolderResponse(folder);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get All Folders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all non-deleted folders for the authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FolderResponse> getAllFolders(Long ownerId) {
        log.debug("Fetching all folders for ownerId={}", ownerId);

        return folderRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .stream()
                .map(folderMapper::toFolderResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Root Folders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all root-level folders (parentFolderId is null) for the
     * authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FolderResponse> getRootFolders(Long ownerId) {
        log.debug("Fetching root folders for ownerId={}", ownerId);

        // Root folders are those where parentFolderId is null and not deleted
        return folderRepository.findByOwnerIdAndDeletedFalse(ownerId)
                .stream()
                .filter(folder -> folder.getParentFolderId() == null)
                .map(folderMapper::toFolderResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Folder Children
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the immediate children of a specific folder.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolderChildren(UUID parentFolderId, Long ownerId) {
        log.debug("Fetching children for folder: parentFolderId={}, ownerId={}",
                parentFolderId, ownerId);

        // Verify the parent folder exists and belongs to the owner
        findActiveFolderByIdAndOwner(parentFolderId, ownerId);

        return folderRepository.findByOwnerIdAndParentFolderIdAndDeletedFalse(ownerId, parentFolderId)
                .stream()
                .map(folderMapper::toFolderResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trash
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all soft-deleted (trashed) folders for the authenticated user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FolderResponse> getTrashFolders(Long ownerId) {
        log.debug("Fetching trash folders for ownerId={}", ownerId);

        return folderRepository.findByOwnerIdAndDeletedTrue(ownerId)
                .stream()
                .map(folderMapper::toFolderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Restores a soft-deleted folder and recursively restores all child folders.
     */
    @Override
    public FolderResponse restoreFolder(UUID folderId, Long ownerId) {
        log.debug("Restoring folder: id={}, ownerId={}", folderId, ownerId);

        Folder folder = findTrashedFolderByIdAndOwner(folderId, ownerId);
        restoreRecursively(folder);

        log.info("Folder restored successfully: id={}, path='{}'", folderId, folder.getPath());

        return folderMapper.toFolderResponse(folder);
    }

    /**
     * Permanently deletes a soft-deleted folder and recursively deletes all
     * child folders. This cannot be undone.
     */
    @Override
    public void permanentlyDeleteFolder(UUID folderId, Long ownerId) {
        log.debug("Permanently deleting folder: id={}, ownerId={}", folderId, ownerId);

        Folder folder = findTrashedFolderByIdAndOwner(folderId, ownerId);
        hardDeleteRecursively(folder);

        log.info("Folder permanently deleted: id={}, path='{}'", folderId, folder.getPath());
    }

    /**
     * Permanently deletes every trashed folder owned by the user. Whole
     * soft-deleted subtrees are removed once (via their top-most deleted
     * ancestor) so descendant rows are not double-deleted.
     */
    @Override
    public void emptyTrash(Long ownerId) {
        log.debug("Emptying trash for ownerId={}", ownerId);

        List<Folder> trashFolders = folderRepository.findByOwnerIdAndDeletedTrue(ownerId);
        Set<UUID> trashedIds = trashFolders.stream()
                .map(Folder::getId)
                .collect(Collectors.toSet());

        int deleted = 0;
        for (Folder folder : trashFolders) {
            UUID parentId = folder.getParentFolderId();
            // Skip descendants whose parent is also in the trash — they are
            // removed when the top-most deleted ancestor is hard-deleted.
            if (parentId != null && trashedIds.contains(parentId)) {
                continue;
            }
            if (!folderRepository.existsById(folder.getId())) {
                continue;
            }
            hardDeleteRecursively(folder);
            deleted++;
        }

        log.info("Trash emptied: {} folder subtree(s) permanently deleted for ownerId={}",
                deleted, ownerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes the new path for a folder after a rename operation.
     *
     * @param folder  the folder being renamed
     * @param newName the new folder name
     * @return the computed new path
     */
    private String computeNewPath(Folder folder, String newName) {
        if (folder.getParentFolderId() == null) {
            return "/" + newName;
        }
        // Find parent to reconstruct the path prefix
        Folder parent = folderRepository.findById(folder.getParentFolderId())
                .orElseThrow(() -> new FolderNotFoundException(
                        "Parent folder not found with id: " + folder.getParentFolderId()));
        return parent.getPath() + "/" + newName;
    }

    /**
     * Recursively updates the paths of all child folders when a parent's
     * path changes (due to rename or move).
     * <p>
     * Replaces the old path prefix with the new one for all descendants.
     *
     * @param oldPathPrefix the old path prefix to replace
     * @param newPathPrefix the new path prefix to use
     */
    private void updateChildPathsRecursively(String oldPathPrefix, String newPathPrefix) {
        List<Folder> children = folderRepository.findByPathStartingWith(oldPathPrefix + "/");

        for (Folder child : children) {
            String updatedPath = child.getPath().replace(oldPathPrefix, newPathPrefix);

            // Calculate new level based on the new path depth
            int newLevel = updatedPath.length() - updatedPath.replace("/", "").length();

            child.setPath(updatedPath);
            child.setLevel(newLevel);
            folderRepository.save(child);

            log.debug("Updated child path: id={}, oldPath='{}', newPath='{}'",
                    child.getId(), child.getPath(), updatedPath);

            // Recursively update deeper descendants
            updateChildPathsRecursively(child.getPath(), updatedPath);
        }
    }

    /**
     * Recursively soft-deletes a folder and all its descendants.
     *
     * @param folder the folder to soft-delete
     */
    private void softDeleteRecursively(Folder folder) {
        folder.setDeleted(true);
        folderRepository.save(folder);

        List<Folder> children = folderRepository.findByParentFolderId(folder.getId());
        for (Folder child : children) {
            softDeleteRecursively(child);
        }
    }

    /**
     * Checks whether a given folder is a descendant of the specified ancestor.
     * <p>
     * Used for cycle prevention in move operations.
     *
     * @param folderId the folder to check (potential descendant)
     * @param ancestor the potential ancestor folder
     * @return {@code true} if folderId is a descendant of ancestor
     */
    private boolean isDescendant(UUID folderId, Folder ancestor) {
        List<Folder> children = folderRepository.findByParentFolderId(ancestor.getId());
        for (Folder child : children) {
            if (child.getId().equals(folderId)) {
                return true;
            }
            if (isDescendant(folderId, child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Recursively restores a folder and all its descendants by setting the
     * {@code deleted} flag back to {@code false}.
     *
     * @param folder the folder to restore
     */
    private void restoreRecursively(Folder folder) {
        folder.setDeleted(false);
        folderRepository.save(folder);

        for (Folder child : folderRepository.findByParentFolderId(folder.getId())) {
            restoreRecursively(child);
        }
    }

    /**
     * Recursively hard-deletes a folder and all its descendants.
     *
     * @param folder the folder to delete
     */
    private void hardDeleteRecursively(Folder folder) {
        for (Folder child : folderRepository.findByParentFolderId(folder.getId())) {
            hardDeleteRecursively(child);
        }
        folderRepository.delete(folder);
    }

    /**
     * Internal helper to find a trashed (soft-deleted) folder by ID and owner,
     * or throw an appropriate exception.
     *
     * @param id      the folder UUID
     * @param ownerId the owner's UUID
     * @return the found Folder entity
     * @throws FolderNotFoundException         if no folder exists with the given ID and owner
     * @throws InvalidFolderOperationException if the folder is not in the trash
     */
    private Folder findTrashedFolderByIdAndOwner(UUID id, Long ownerId) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Folder not found: id={}", id);
                    return new FolderNotFoundException("Folder not found with id: " + id);
                });

        if (!folder.getDeleted()) {
            log.warn("Folder is not in the trash: id={}", id);
            throw new InvalidFolderOperationException(
                    "Only folders in the trash can be restored or permanently deleted");
        }

        if (!folder.getOwnerId().equals(ownerId)) {
            log.warn("Folder {} does not belong to owner {}", id, ownerId);
            throw new FolderNotFoundException("Folder not found with id: " + id);
        }

        return folder;
    }

    /**
     * Internal helper to find an active (non-deleted) folder by ID and owner,
     * or throw {@link FolderNotFoundException}.
     *
     * @param id      the folder UUID
     * @param ownerId the owner's UUID
     * @return the found Folder entity
     * @throws FolderNotFoundException if no active folder exists with the given ID and owner
     */
    private Folder findActiveFolderByIdAndOwner(UUID id, Long ownerId) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Folder not found: id={}", id);
                    return new FolderNotFoundException("Folder not found with id: " + id);
                });

        if (folder.getDeleted()) {
            log.warn("Folder is deleted: id={}", id);
            throw new FolderNotFoundException("Folder not found with id: " + id);
        }

        if (!folder.getOwnerId().equals(ownerId)) {
            log.warn("Folder {} does not belong to owner {}", id, ownerId);
            throw new FolderNotFoundException("Folder not found with id: " + id);
        }

        return folder;
    }
}
