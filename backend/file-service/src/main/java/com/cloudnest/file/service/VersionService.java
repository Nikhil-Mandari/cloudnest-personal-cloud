package com.cloudnest.file.service;

import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.FileVersionResponse;
import com.cloudnest.file.entity.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for file version history.
 * <p>
 * The current content of a file always lives on the file record itself; every
 * content replacement archives the previous content as a version row. Restore
 * swaps the file's object pointer back to a snapshot.
 */
public interface VersionService {

    /**
     * Lists the archived versions of a file, newest first.
     */
    List<FileVersionResponse> getVersions(Long fileId, Long ownerId);

    /**
     * Uploads a new version: archives the current content, replaces the file's
     * content with the uploaded one.
     */
    FileResponse uploadNewVersion(Long fileId, MultipartFile file, Long ownerId);

    /**
     * Restores an archived version: archives the current content, then points
     * the file at the restored snapshot.
     */
    FileResponse restoreVersion(Long fileId, Long versionId, Long ownerId);

    /**
     * Deletes an archived version (its content is removed from MinIO).
     * The version holding the file's current content cannot be deleted.
     */
    void deleteVersion(Long fileId, Long versionId, Long ownerId);

    /**
     * Streams an archived version's content for download.
     */
    FileDownloadResponse downloadVersion(Long fileId, Long versionId, Long ownerId);

    /**
     * Archives the file's current content as the next version snapshot.
     * Called when content is replaced (new version, duplicate REPLACE).
     */
    void archiveCurrent(FileMetadata file, Long ownerId);
}
