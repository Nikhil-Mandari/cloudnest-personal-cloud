package com.cloudnest.folder.controller;

import com.cloudnest.folder.dto.CreateFolderRequest;
import com.cloudnest.folder.dto.FolderResponse;
import com.cloudnest.folder.dto.MoveFolderRequest;
import com.cloudnest.folder.dto.UpdateFolderRequest;
import com.cloudnest.folder.service.FolderService;
import com.cloudnest.folder.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for folder management operations.
 * <p>
 * Provides endpoints for creating, retrieving, renaming, moving, deleting,
 * and listing folders in a hierarchical structure.
 * The authenticated user's ID is received via the {@code X-User-Id} header,
 * which is set by the API Gateway after JWT authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    /**
     * Creates a new folder.
     * <p>
     * The owner ID is extracted from the authenticated JWT (forwarded by
     * API Gateway), never from the request body.
     *
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param request      the creation payload (name + optional parentFolderId)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 201 Created with the newly created folder
     */
    @PostMapping
    public ResponseEntity<StandardResponse<FolderResponse>> createFolder(
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody CreateFolderRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/folders - userId={}, name='{}', parentFolderId={}",
                userIdHeader, request.getName(), request.getParentFolderId());

        FolderResponse response = folderService.createFolder(userIdHeader, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<FolderResponse>builder()
                        .success(true)
                        .message("Folder created successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Renames a folder and recursively updates all child folder paths.
     *
     * @param id           the UUID of the folder to rename
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param request      the rename payload containing the new name
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated folder
     */
    @PutMapping("/{id}")
    public ResponseEntity<StandardResponse<FolderResponse>> renameFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody UpdateFolderRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/folders/{} - userId={}, newName='{}'",
                id, userIdHeader, request.getName());

        FolderResponse response = folderService.renameFolder(id, userIdHeader, request);

        return ResponseEntity.ok(
                StandardResponse.<FolderResponse>builder()
                        .success(true)
                        .message("Folder renamed successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Soft-deletes a folder and recursively soft-deletes all child folders.
     *
     * @param id           the UUID of the folder to delete
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> deleteFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/folders/{} - userId={}", id, userIdHeader);

        folderService.deleteFolder(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Folder deleted successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Lists all soft-deleted (trashed) folders for the authenticated user.
     *
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of trashed folders
     */
    @GetMapping("/trash")
    public ResponseEntity<StandardResponse<List<FolderResponse>>> getTrashFolders(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/folders/trash - userId={}", userIdHeader);

        List<FolderResponse> trashFolders = folderService.getTrashFolders(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FolderResponse>>builder()
                        .success(true)
                        .message("Trashed folders retrieved successfully")
                        .data(trashFolders)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Restores a soft-deleted folder (and its descendants) from the trash.
     *
     * @param id           the UUID of the folder to restore
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the restored folder
     */
    @PatchMapping("/{id}/restore")
    public ResponseEntity<StandardResponse<FolderResponse>> restoreFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("PATCH /api/folders/{}/restore - userId={}", id, userIdHeader);

        FolderResponse response = folderService.restoreFolder(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FolderResponse>builder()
                        .success(true)
                        .message("Folder restored successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Permanently deletes a soft-deleted folder and all its descendants.
     *
     * @param id           the UUID of the folder to delete
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<StandardResponse<Void>> permanentlyDeleteFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/folders/{}/permanent - userId={}", id, userIdHeader);

        folderService.permanentlyDeleteFolder(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Folder permanently deleted")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Permanently deletes every trashed folder owned by the authenticated user.
     *
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the operation
     */
    @DeleteMapping("/trash")
    public ResponseEntity<StandardResponse<Void>> emptyTrash(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/folders/trash - userId={}", userIdHeader);

        folderService.emptyTrash(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Trash emptied successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves a single folder by its UUID.
     *
     * @param id           the UUID of the folder
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the folder details
     */
    @GetMapping("/{id}")
    public ResponseEntity<StandardResponse<FolderResponse>> getFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/folders/{} - userId={}", id, userIdHeader);

        FolderResponse response = folderService.getFolder(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<FolderResponse>builder()
                        .success(true)
                        .message("Folder retrieved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves all non-deleted folders for the authenticated user.
     *
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of folders
     */
    @GetMapping
    public ResponseEntity<StandardResponse<List<FolderResponse>>> getAllFolders(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/folders - userId={}", userIdHeader);

        List<FolderResponse> folders = folderService.getAllFolders(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FolderResponse>>builder()
                        .success(true)
                        .message("Folders retrieved successfully")
                        .data(folders)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves all root-level folders (parentFolderId is null) for
     * the authenticated user.
     *
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of root folders
     */
    @GetMapping("/root")
    public ResponseEntity<StandardResponse<List<FolderResponse>>> getRootFolders(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/folders/root - userId={}", userIdHeader);

        List<FolderResponse> rootFolders = folderService.getRootFolders(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FolderResponse>>builder()
                        .success(true)
                        .message("Root folders retrieved successfully")
                        .data(rootFolders)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves the immediate children of a specific folder.
     *
     * @param id           the UUID of the parent folder
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of child folders
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<StandardResponse<List<FolderResponse>>> getFolderChildren(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/folders/{}/children - userId={}", id, userIdHeader);

        List<FolderResponse> children = folderService.getFolderChildren(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<FolderResponse>>builder()
                        .success(true)
                        .message("Folder children retrieved successfully")
                        .data(children)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Moves a folder to a different parent folder.
     * <p>
     * Validates that the move does not create a cycle and recursively
     * updates all child folder paths.
     *
     * @param id           the UUID of the folder to move
     * @param userIdHeader the authenticated user's UUID (set by API Gateway from JWT)
     * @param request      the move payload containing the destination folder ID
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated folder
     */
    @PutMapping("/{id}/move")
    public ResponseEntity<StandardResponse<FolderResponse>> moveFolder(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            @Valid @RequestBody MoveFolderRequest request,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/folders/{}/move - userId={}, destinationFolderId={}",
                id, userIdHeader, request.getDestinationFolderId());

        FolderResponse response = folderService.moveFolder(id, userIdHeader, request);

        return ResponseEntity.ok(
                StandardResponse.<FolderResponse>builder()
                        .success(true)
                        .message("Folder moved successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
