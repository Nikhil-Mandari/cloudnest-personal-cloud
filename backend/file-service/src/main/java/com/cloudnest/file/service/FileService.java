package com.cloudnest.file.service;

import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for file operations.
 * <p>
 * Defines the contract for the MinIO-backed file lifecycle: binary content is
 * stored in MinIO object storage while metadata is persisted in MySQL.
 * Ownership of every file is enforced against the authenticated user.
 */
public interface FileService {

    /**
     * Uploads a file: validates it, computes its SHA-256 checksum, uploads the
     * binary content to MinIO, and persists the metadata in MySQL.
     *
     * @param request the metadata derived from the multipart upload
     * @param file    the multipart file to upload
     * @return the created file metadata in detailed response format
     */
    FileResponse uploadFile(UploadFileRequest request, MultipartFile file);

    /**
     * Retrieves all active file metadata records for a specific owner.
     *
     * @param ownerId the ID of the file owner
     * @return a list of lightweight file metadata responses
     */
    List<FileMetadataResponse> getUserFiles(Long ownerId);

    /**
     * Retrieves the active file metadata records for a specific owner, scoped
     * to one explorer location.
     *
     * @param ownerId  the ID of the file owner
     * @param folderId {@code null} = every active file (dashboard / global view),
     *                 {@code ""} / blank = root-level files only, a UUID = the
     *                 files inside that folder
     * @return a list of lightweight file metadata responses
     */
    List<FileMetadataResponse> getUserFiles(Long ownerId, String folderId);

    /**
     * Retrieves detailed file metadata by its internal ID.
     * <p>
     * When an owner context is supplied, ownership is enforced; when absent
     * (internal Feign calls, e.g. from the Share Service) access is allowed.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID, or {@code null} for internal access
     * @return the detailed file metadata response
     */
    FileResponse getFileById(Long id, Long ownerId);

    /**
     * Retrieves detailed file metadata by its public-facing file ID (UUID).
     *
     * @param fileId the unique file identifier
     * @return the detailed file metadata response
     */
    FileResponse getFileByFileId(String fileId);

    /**
     * Updates select fields of an existing file metadata record (rename).
     * <p>
     * Only metadata is updated — the MinIO object key is never renamed.
     *
     * @param id      the internal primary key of the file record
     * @param request the update payload with optional fields
     * @param ownerId the authenticated user's ID
     * @return the updated file metadata in detailed response format
     */
    FileResponse updateFileDetails(Long id, UpdateFileRequest request, Long ownerId);

    /**
     * Moves a file to a different folder.
     * <p>
     * Only the {@code folderId} is updated — the MinIO object is never moved.
     *
     * @param id          the internal primary key of the file record
     * @param newFolderId the target folder UUID (null moves the file to root)
     * @param ownerId     the authenticated user's ID
     * @return the updated file metadata in detailed response format
     */
    FileResponse moveFile(Long id, String newFolderId, Long ownerId);

    /**
     * Soft-deletes a file by moving it to the trash: the metadata status is set
     * to {@code DELETED} and the MinIO object is retained so the file can be
     * restored from the trash.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID
     */
    void deleteFile(Long id, Long ownerId);

    /**
     * Restores a soft-deleted (trashed) file record by setting its status back
     * to {@code ACTIVE}.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID
     * @return the restored file metadata in detailed response format
     */
    FileResponse restoreFile(Long id, Long ownerId);

    /**
     * Retrieves all soft-deleted (trashed) file metadata records for an owner.
     *
     * @param ownerId the ID of the file owner
     * @return a list of lightweight file metadata responses
     */
    List<FileMetadataResponse> getTrashFiles(Long ownerId);

    /**
     * Permanently deletes a trashed file: removes the object from MinIO and
     * deletes the metadata row from MySQL. Only files currently in the trash
     * can be permanently deleted.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID
     */
    void permanentlyDeleteFile(Long id, Long ownerId);

    /**
     * Permanently deletes every trashed file owned by the user (empty trash).
     * A single failure is logged and skipped so the rest of the trash is still
     * cleared.
     *
     * @param ownerId the authenticated user's ID
     */
    void emptyTrash(Long ownerId);

    /**
     * Streams a file's binary content from MinIO for download.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID
     * @return the streamed content together with its metadata
     */
    FileDownloadResponse downloadFile(Long id, Long ownerId);

    /**
     * Streams a file's binary content from MinIO for in-browser preview.
     * Only previewable content types are supported.
     *
     * @param id      the internal primary key of the file record
     * @param ownerId the authenticated user's ID
     * @return the streamed content together with its metadata
     */
    FileDownloadResponse previewFile(Long id, Long ownerId);

    /**
     * Retrieves all active file metadata records marked as favorite by an owner.
     *
     * @param ownerId the ID of the file owner
     * @return a list of favorite lightweight file metadata responses
     */
    List<FileMetadataResponse> getFavoriteFiles(Long ownerId);

    /**
     * Marks (or unmarks) a file as favorite. When {@code favorite} is
     * {@code null}, the current value is toggled.
     *
     * @param id       the internal primary key of the file record
     * @param favorite the target favorite state, or {@code null} to toggle
     * @param ownerId  the authenticated user's ID
     * @return the updated file metadata in detailed response format
     */
    FileResponse setFavorite(Long id, Boolean favorite, Long ownerId);

    /**
     * Searches for active file records by original file name (case-insensitive).
     *
     * @param query   the search term
     * @param ownerId the ID of the file owner to scope the search
     * @return a list of matching lightweight file metadata responses
     */
    List<FileMetadataResponse> searchFiles(String query, Long ownerId);
}
