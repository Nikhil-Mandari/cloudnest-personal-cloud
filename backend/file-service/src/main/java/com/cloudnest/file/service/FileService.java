package com.cloudnest.file.service;

import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;

import java.util.List;

/**
 * Service interface for file metadata operations.
 * <p>
 * Defines the contract for managing file metadata records including
 * CRUD, soft-delete, restore, search, and move operations.
 * <strong>Note:</strong> This service manages metadata only — binary
 * storage is not handled here.
 */
public interface FileService {

    /**
     * Registers new file metadata after an upload.
     *
     * @param request the upload request containing file metadata
     * @return the created file metadata in detailed response format
     */
    FileResponse uploadFileMetadata(UploadFileRequest request);

    /**
     * Retrieves all active file metadata records for a specific owner.
     *
     * @param ownerId the ID of the file owner
     * @return a list of lightweight file metadata responses
     */
    List<FileMetadataResponse> getUserFiles(Long ownerId);

    /**
     * Retrieves detailed file metadata by its internal ID.
     *
     * @param id the internal primary key of the file record
     * @return the detailed file metadata response
     */
    FileResponse getFileById(Long id);

    /**
     * Retrieves detailed file metadata by its public-facing file ID (UUID).
     *
     * @param fileId the unique file identifier
     * @return the detailed file metadata response
     */
    FileResponse getFileByFileId(String fileId);

    /**
     * Updates select fields of an existing file metadata record.
     *
     * @param id      the internal primary key of the file record
     * @param request the update payload with optional fields
     * @return the updated file metadata in detailed response format
     */
    FileResponse updateFileDetails(Long id, UpdateFileRequest request);

    /**
     * Moves a file to a different folder.
     *
     * @param id          the internal primary key of the file record
     * @param newFolderId the target folder ID (may be null to move to root)
     * @return the updated file metadata in detailed response format
     */
    FileResponse moveFile(Long id, Long newFolderId);

    /**
     * Soft-deletes a file record by setting its status to {@code DELETED}.
     *
     * @param id the internal primary key of the file record
     */
    void deleteFile(Long id);

    /**
     * Restores a soft-deleted file record by setting its status back to {@code ACTIVE}.
     *
     * @param id the internal primary key of the file record
     * @return the restored file metadata in detailed response format
     */
    FileResponse restoreFile(Long id);

    /**
     * Searches for active file records by original file name (case-insensitive).
     *
     * @param query   the search term
     * @param ownerId the ID of the file owner to scope the search
     * @return a list of matching lightweight file metadata responses
     */
    List<FileMetadataResponse> searchFiles(String query, Long ownerId);
}
