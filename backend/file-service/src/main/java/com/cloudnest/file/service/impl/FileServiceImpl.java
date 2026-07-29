package com.cloudnest.file.service.impl;

import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.exception.BadRequestException;
import com.cloudnest.file.exception.DuplicateResourceException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.mapper.FileMapper;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link FileService} interface.
 * <p>
 * Handles all file metadata management operations including CRUD,
 * soft-delete, restore, search, and folder movement.
 */
@Slf4j
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMapper fileMapper;

    public FileServiceImpl(FileMetadataRepository fileMetadataRepository, FileMapper fileMapper) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileMapper = fileMapper;
    }

    /**
     * Registers new file metadata after an upload.
     * Generates a unique file ID (UUID) and a stored file name to prevent collisions.
     */
    @Override
    public FileResponse uploadFileMetadata(UploadFileRequest request) {
        log.debug("Uploading file metadata: originalFileName='{}', ownerId={}",
                request.getOriginalFileName(), request.getOwnerId());

        // ── Generate a unique file ID and stored file name ──────────────────────
        String fileId = UUID.randomUUID().toString();
        String storedFileName = generateStoredFileName(request.getOriginalFileName());

        // ── Ensure stored file name uniqueness ──────────────────────────────────
        if (fileMetadataRepository.existsByStoredFileName(storedFileName)) {
            log.warn("Stored file name collision: {}", storedFileName);
            throw new DuplicateResourceException(
                    "Stored file name '" + storedFileName + "' already exists");
        }

        // ── Map request to entity and populate generated fields ─────────────────
        FileMetadata fileMetadata = fileMapper.toEntity(request);
        fileMetadata.setFileId(fileId);
        fileMetadata.setStoredFileName(storedFileName);

        // ── Persist and return ──────────────────────────────────────────────────
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);
        log.info("File metadata uploaded successfully: fileId={}, storedFileName={}",
                saved.getFileId(), saved.getStoredFileName());

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Retrieves all active file metadata records for a specific owner.
     * Filters out soft-deleted records.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getUserFiles(Long ownerId) {
        log.debug("Fetching files for ownerId={}", ownerId);

        return fileMetadataRepository.findByOwnerId(ownerId)
                .stream()
                .filter(file -> file.getStatus() == FileStatus.ACTIVE)
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed file metadata by its internal ID.
     * Includes soft-deleted records for admin/internal use.
     */
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(Long id) {
        log.debug("Fetching file by id={}", id);

        FileMetadata fileMetadata = findFileById(id);
        return fileMapper.toFileResponse(fileMetadata);
    }

    /**
     * Retrieves detailed file metadata by its public-facing file ID (UUID).
     * Includes soft-deleted records for admin/internal use.
     */
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileByFileId(String fileId) {
        log.debug("Fetching file by fileId={}", fileId);

        FileMetadata fileMetadata = fileMetadataRepository.findByFileId(fileId)
                .orElseThrow(() -> {
                    log.warn("File not found: fileId={}", fileId);
                    return new ResourceNotFoundException("File not found with fileId: " + fileId);
                });

        return fileMapper.toFileResponse(fileMetadata);
    }

    /**
     * Updates select fields of an existing file metadata record.
     * Only the fields provided in the request are updated.
     */
    @Override
    public FileResponse updateFileDetails(Long id, UpdateFileRequest request) {
        log.debug("Updating file metadata: id={}", id);

        FileMetadata fileMetadata = findFileById(id);

        // ── Check for soft-deleted files ────────────────────────────────────────
        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            throw new BadRequestException("Cannot update a deleted file. Restore it first.");
        }

        // ── Apply partial update via MapStruct ──────────────────────────────────
        fileMapper.applyUpdate(fileMetadata, request);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        log.info("File metadata updated successfully: id={}, fileId={}",
                saved.getId(), saved.getFileId());

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Moves a file to a different folder.
     */
    @Override
    public FileResponse moveFile(Long id, Long newFolderId) {
        log.debug("Moving file: id={}, newFolderId={}", id, newFolderId);

        FileMetadata fileMetadata = findFileById(id);

        // ── Check for soft-deleted files ────────────────────────────────────────
        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            throw new BadRequestException("Cannot move a deleted file. Restore it first.");
        }

        fileMetadata.setFolderId(newFolderId);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        log.info("File moved successfully: id={}, fileId={}, newFolderId={}",
                saved.getId(), saved.getFileId(), newFolderId);

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Soft-deletes a file record by setting its status to {@code DELETED}.
     */
    @Override
    public void deleteFile(Long id) {
        log.debug("Soft-deleting file: id={}", id);

        FileMetadata fileMetadata = findFileById(id);

        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            log.warn("File is already deleted: id={}", id);
            throw new BadRequestException("File is already deleted");
        }

        fileMetadata.setStatus(FileStatus.DELETED);
        fileMetadataRepository.save(fileMetadata);

        log.info("File soft-deleted successfully: id={}, fileId={}",
                id, fileMetadata.getFileId());
    }

    /**
     * Restores a soft-deleted file record by setting its status back to {@code ACTIVE}.
     */
    @Override
    public FileResponse restoreFile(Long id) {
        log.debug("Restoring soft-deleted file: id={}", id);

        FileMetadata fileMetadata = findFileById(id);

        if (fileMetadata.getStatus() == FileStatus.ACTIVE) {
            log.warn("File is already active: id={}", id);
            throw new BadRequestException("File is already active — no restoration needed");
        }

        fileMetadata.setStatus(FileStatus.ACTIVE);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        log.info("File restored successfully: id={}, fileId={}",
                saved.getId(), saved.getFileId());

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Searches for active file records by original file name (case-insensitive).
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> searchFiles(String query, Long ownerId) {
        log.debug("Searching files: query='{}', ownerId={}", query, ownerId);

        if (query == null || query.trim().isEmpty()) {
            log.debug("Empty search query — returning all active files for ownerId={}", ownerId);
            return getUserFiles(ownerId);
        }

        // Remove the filter on status since the repository query already filters for ACTIVE
        List<FileMetadata> results = fileMetadataRepository.searchByFileName(query.trim(), ownerId);

        log.debug("Search found {} files for query='{}'", results.size(), query);

        return results.stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Generates a unique stored file name by prefixing a UUID to the original file name.
     *
     * @param originalFileName the original file name
     * @return a unique stored file name
     */
    private String generateStoredFileName(String originalFileName) {
        return UUID.randomUUID() + "_" + originalFileName;
    }

    /**
     * Internal helper to find a file metadata record by ID or throw {@link ResourceNotFoundException}.
     *
     * @param id the internal primary key
     * @return the found FileMetadata entity
     * @throws ResourceNotFoundException if no record exists with the given ID
     */
    private FileMetadata findFileById(Long id) {
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("File not found: id={}", id);
                    return new ResourceNotFoundException("File not found with id: " + id);
                });
    }
}
