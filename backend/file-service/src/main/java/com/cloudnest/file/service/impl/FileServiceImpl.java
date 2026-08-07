package com.cloudnest.file.service.impl;

import com.cloudnest.file.client.FolderServiceClient;
import com.cloudnest.file.client.ShareServiceClient;
import com.cloudnest.file.config.MinioProperties;
import com.cloudnest.file.dto.DownloadZipRequest;
import com.cloudnest.file.dto.DuplicateFileInfo;
import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.FolderResponse;
import com.cloudnest.file.dto.PagedAuditLogsResponse;
import com.cloudnest.file.dto.ScanStatusResponse;
import com.cloudnest.file.dto.ShareValidationResponse;
import com.cloudnest.file.dto.StorageOverviewResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.dto.UploadResultResponse;
import com.cloudnest.file.entity.AuditLog.AuditAction;
import com.cloudnest.file.entity.DuplicateAction;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.entity.ScanStatus;
import com.cloudnest.file.exception.BadRequestException;
import com.cloudnest.file.exception.DuplicateResourceException;
import com.cloudnest.file.exception.FileStorageException;
import com.cloudnest.file.exception.FileTooLargeException;
import com.cloudnest.file.exception.ForbiddenException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.exception.UnauthorizedException;
import com.cloudnest.file.exception.VirusDetectedException;
import com.cloudnest.file.mapper.FileMapper;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.service.AuditLogService;
import com.cloudnest.file.service.FileService;
import com.cloudnest.file.service.MinioService;
import com.cloudnest.file.service.StorageAnalyticsService;
import com.cloudnest.file.service.VersionService;
import com.cloudnest.file.service.VirusScanService;
import com.cloudnest.file.service.ZipDownloadService;
import com.cloudnest.file.util.ChecksumUtil;
import com.cloudnest.file.util.FileNameUtil;
import com.cloudnest.file.util.StandardResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link FileService} interface.
 * <p>
 * Orchestrates the MinIO-backed file lifecycle: binary content is uploaded to /
 * read from MinIO object storage, while metadata (object key, bucket, content
 * type, size, SHA-256 checksum, virus-scan status) is persisted in MySQL.
 * Every mutation is ownership-checked against the authenticated user and
 * appended to the audit trail.
 */
@Slf4j
@Service
@Transactional
public class FileServiceImpl implements FileService {

    /** MIME types that support in-browser preview. */
    private static final Set<String> PREVIEWABLE_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "text/plain"
    );

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMapper fileMapper;
    private final MinioService minioService;
    private final MinioProperties minioProperties;
    private final FolderServiceClient folderServiceClient;
    private final ShareServiceClient shareServiceClient;
    private final VirusScanService virusScanService;
    private final AuditLogService auditLogService;
    private final VersionService versionService;
    private final StorageAnalyticsService storageAnalyticsService;
    private final ZipDownloadService zipDownloadService;

    public FileServiceImpl(
            FileMetadataRepository fileMetadataRepository,
            FileMapper fileMapper,
            MinioService minioService,
            MinioProperties minioProperties,
            FolderServiceClient folderServiceClient,
            ShareServiceClient shareServiceClient,
            VirusScanService virusScanService,
            AuditLogService auditLogService,
            VersionService versionService,
            StorageAnalyticsService storageAnalyticsService,
            ZipDownloadService zipDownloadService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileMapper = fileMapper;
        this.minioService = minioService;
        this.minioProperties = minioProperties;
        this.folderServiceClient = folderServiceClient;
        this.shareServiceClient = shareServiceClient;
        this.virusScanService = virusScanService;
        this.auditLogService = auditLogService;
        this.versionService = versionService;
        this.storageAnalyticsService = storageAnalyticsService;
        this.zipDownloadService = zipDownloadService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the upload, computes the SHA-256 checksum, detects duplicate
     * content for the same owner, virus-scans, uploads the binary content to
     * MinIO, then persists the metadata in MySQL.
     * <p>
     * The {@code onDuplicate} action decides what happens when identical
     * content already exists:
     * <ul>
     *   <li>{@code ASK} — detect and report without uploading (client decides)</li>
     *   <li>{@code KEEP_BOTH} — always create a new file record</li>
     *   <li>{@code SKIP} — do not upload</li>
     *   <li>{@code REPLACE} — archive the existing file's content as a version
     *       and replace it with the new content</li>
     * </ul>
     * If the virus scan flags the content as infected, the uploaded object is
     * removed and {@link VirusDetectedException} is thrown.
     */
    @Override
    public UploadResultResponse uploadFile(UploadFileRequest request, MultipartFile file, DuplicateAction onDuplicate) {
        log.debug("Uploading file: originalFileName='{}', ownerId={}, folderId={}, onDuplicate={}",
                request.getOriginalFileName(), request.getOwnerId(), request.getFolderId(), onDuplicate);

        // ── Validate the request ─────────────────────────────────────────────
        validateOwner(request.getOwnerId());
        validateFileForUpload(file);
        validateFolder(request.getFolderId(), request.getOwnerId());

        // ── Derive storage metadata ───────────────────────────────────────────
        String originalFileName = FileNameUtil.sanitizeFileName(request.getOriginalFileName());
        String contentType = resolveContentType(request.getContentType());

        // ── Compute SHA-256 checksum while the content is available ───────────
        String checksum;
        try (InputStream checksumStream = file.getInputStream()) {
            checksum = ChecksumUtil.sha256Hex(checksumStream);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }
        request.setChecksum(checksum);

        // ── Duplicate detection (identical content, same owner) ───────────────
        List<FileMetadata> duplicates = fileMetadataRepository
                .findByChecksumAndOwnerIdAndStatus(checksum, request.getOwnerId(), FileStatus.ACTIVE);
        FileMetadata existing = duplicates.stream().findFirst().orElse(null);

        if (existing != null && onDuplicate != DuplicateAction.KEEP_BOTH) {
            if (onDuplicate == DuplicateAction.SKIP) {
                auditLogService.record(request.getOwnerId(), AuditAction.UPLOAD_DUPLICATE_SKIPPED, "FILE",
                        String.valueOf(existing.getId()), existing.getOriginalFileName(),
                        "Skipped duplicate upload (checksum " + shortChecksum(checksum) + ")");
                log.info("Duplicate upload skipped: ownerId={}, existingFileId={}, checksum={}",
                        request.getOwnerId(), existing.getId(), checksum);
                return UploadResultResponse.builder()
                        .duplicate(true)
                        .actionTaken(DuplicateAction.SKIP)
                        .duplicateOf(toDuplicateInfo(existing))
                        .build();
            }

            if (onDuplicate == DuplicateAction.ASK) {
                // Report the duplicate WITHOUT uploading — the client decides.
                auditLogService.record(request.getOwnerId(), AuditAction.UPLOAD_DUPLICATE_SKIPPED, "FILE",
                        String.valueOf(existing.getId()), existing.getOriginalFileName(),
                        "Duplicate content detected — awaiting user decision (checksum "
                                + shortChecksum(checksum) + ")");
                log.info("Duplicate content detected: ownerId={}, existingFileId={}, checksum={}",
                        request.getOwnerId(), existing.getId(), checksum);
                return UploadResultResponse.builder()
                        .duplicate(true)
                        .actionTaken(DuplicateAction.ASK)
                        .duplicateOf(toDuplicateInfo(existing))
                        .build();
            }

            // ── REPLACE: upload new content, archive the existing content ─────
            String objectName = FileNameUtil.generateObjectName(originalFileName);
            uploadAndScan(file, objectName, contentType);

            versionService.archiveCurrent(existing, request.getOwnerId());

            existing.setObjectName(objectName);
            existing.setStoredFileName(objectName);
            existing.setChecksum(checksum);
            existing.setFileSize(file.getSize());
            existing.setContentType(contentType);
            existing.setFileType(contentType);
            existing.setUploadedAt(LocalDateTime.now());
            existing.setScanStatus(ScanStatus.CLEAN);

            FileMetadata saved;
            try {
                saved = fileMetadataRepository.save(existing);
            } catch (RuntimeException e) {
                log.error("Duplicate replace failed for object '{}' — rolling back MinIO upload: {}",
                        objectName, e.getMessage());
                try {
                    minioService.deleteObject(objectName);
                } catch (RuntimeException rollbackEx) {
                    log.error("Failed to roll back object '{}' after replace failure",
                            objectName, rollbackEx);
                }
                throw e;
            }

            auditLogService.record(request.getOwnerId(), AuditAction.UPLOAD_REPLACED, "FILE",
                    String.valueOf(saved.getId()), saved.getOriginalFileName(),
                    "Replaced duplicate content (" + saved.getFileSize() + " bytes)");

            log.info("Duplicate replaced: fileId={}, newObjectName={}, size={}",
                    saved.getId(), objectName, file.getSize());

            return UploadResultResponse.builder()
                    .duplicate(true)
                    .actionTaken(DuplicateAction.REPLACE)
                    .duplicateOf(toDuplicateInfo(saved))
                    .file(fileMapper.toFileResponse(saved))
                    .build();
        }

        // ── KEEP_BOTH (or no duplicate): normal upload ────────────────────────
        String objectName = FileNameUtil.generateObjectName(originalFileName);
        uploadAndScan(file, objectName, contentType);

        // ── Persist metadata into MySQL (rollback object on failure) ──────────
        FileMetadata metadata = fileMapper.toEntity(request);
        metadata.setFileId(UUID.randomUUID().toString());
        metadata.setObjectName(objectName);
        metadata.setStoredFileName(objectName);
        metadata.setBucketName(minioProperties.getBucketName());
        metadata.setStoragePath(minioProperties.getBucketName() + "/" + objectName);
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setScanStatus(ScanStatus.CLEAN);

        FileMetadata saved;
        try {
            saved = fileMetadataRepository.save(metadata);
        } catch (RuntimeException e) {
            log.error("Metadata persistence failed for object '{}' — rolling back MinIO upload: {}",
                    objectName, e.getMessage());
            try {
                minioService.deleteObject(objectName);
            } catch (RuntimeException rollbackEx) {
                log.error("Failed to roll back object '{}' after metadata persistence failure",
                        objectName, rollbackEx);
            }
            throw e;
        }

        auditLogService.record(request.getOwnerId(), AuditAction.UPLOAD, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(),
                saved.getFileSize() + " bytes");

        log.info("File uploaded successfully: id={}, fileId={}, objectName={}, size={}, checksum={}",
                saved.getId(), saved.getFileId(), saved.getObjectName(), saved.getFileSize(),
                saved.getChecksum());

        return UploadResultResponse.builder()
                .duplicate(existing != null)
                .actionTaken(DuplicateAction.KEEP_BOTH)
                .duplicateOf(existing != null ? toDuplicateInfo(existing) : null)
                .file(fileMapper.toFileResponse(saved))
                .build();
    }

    /**
     * Uploads the multipart content to MinIO and virus-scans it. On infection
     * the object is removed and the upload aborts.
     */
    private void uploadAndScan(MultipartFile file, String objectName, String contentType) {
        try (InputStream uploadStream = file.getInputStream()) {
            minioService.uploadObject(objectName, uploadStream, file.getSize(), contentType);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }

        ScanStatus scanStatus;
        try (InputStream scanStream = file.getInputStream()) {
            scanStatus = virusScanService.scan(scanStream, objectName);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }

        if (scanStatus == ScanStatus.INFECTED) {
            minioService.deleteObject(objectName);
            throw new VirusDetectedException(
                    "A virus was detected in the uploaded content — the file was blocked");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all active file metadata records for a specific owner.
     * Filtering happens in SQL so soft-deleted (and legacy NULL-status) rows
     * are never mapped — this also keeps the response free of 500s caused by
     * null enum values reaching the mapper.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getUserFiles(Long ownerId) {
        log.debug("Fetching files for ownerId={}", ownerId);

        return fileMetadataRepository.findActiveByOwnerId(ownerId)
                .stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all ACTIVE file metadata records for a specific owner within a
     * specific folder (or at the root when {@code folderId} is blank).
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getUserFilesByFolder(Long ownerId, String folderId) {
        log.debug("Fetching files for ownerId={}, folderId={}", ownerId, folderId);

        List<FileMetadata> files = (folderId == null || folderId.isBlank())
                ? fileMetadataRepository.findActiveRootFilesByOwnerId(ownerId)
                : fileMetadataRepository.findActiveByOwnerIdAndFolderId(ownerId, folderId);

        return files.stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed file metadata by its internal ID.
     * <p>
     * Ownership is enforced when an owner context is supplied (external API
     * calls); internal Feign consumers (e.g. Share Service) pass {@code null}.
     */
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(Long id, Long ownerId) {
        log.debug("Fetching file by id={}", id);

        FileMetadata fileMetadata = findFileById(id);
        assertOwner(fileMetadata, ownerId);
        return fileMapper.toFileResponse(fileMetadata);
    }

    /**
     * Retrieves detailed file metadata by its public-facing file ID (UUID).
     * Internal access — no ownership context.
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

    // ─────────────────────────────────────────────────────────────────────────
    // Rename (metadata only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates select fields of an existing file metadata record.
     * Only the fields provided in the request are updated.
     * The MinIO object key is never renamed.
     */
    @Override
    public FileResponse updateFileDetails(Long id, UpdateFileRequest request, Long ownerId) {
        log.debug("Updating file metadata: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);
        String previousName = fileMetadata.getOriginalFileName();

        // ── Apply partial update via MapStruct ──────────────────────────────────
        fileMapper.applyUpdate(fileMetadata, request);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        auditLogService.record(ownerId, AuditAction.RENAME, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(),
                "Renamed from '" + previousName + "'");

        log.info("File metadata updated successfully: id={}, fileId={}",
                saved.getId(), saved.getFileId());

        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Move (metadata only)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Moves a file to a different folder — only the {@code folderId} is
     * updated; the MinIO object is never moved.
     */
    @Override
    public FileResponse moveFile(Long id, String newFolderId, Long ownerId) {
        log.debug("Moving file: id={}, newFolderId={}, ownerId={}", id, newFolderId, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);

        // ── Validate the destination folder (if moving into one) ──────────────
        validateFolder(newFolderId, ownerId);

        fileMetadata.setFolderId(newFolderId);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        auditLogService.record(ownerId, AuditAction.MOVE, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(),
                newFolderId != null ? "Moved to folder " + newFolderId : "Moved to root");

        log.info("File moved successfully: id={}, fileId={}, newFolderId={}",
                saved.getId(), saved.getFileId(), newFolderId);

        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete (soft delete to trash) / Restore / Trash management
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a file by moving it to the trash: only the metadata status
     * changes to {@code DELETED}; the MinIO object is retained so the file can
     * be restored from the trash later.
     */
    @Override
    public void deleteFile(Long id, Long ownerId) {
        log.debug("Soft-deleting file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findOwnedFile(id, ownerId);

        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            log.warn("File is already in the trash: id={}", id);
            throw new BadRequestException("File is already in the trash");
        }

        fileMetadata.setStatus(FileStatus.DELETED);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        auditLogService.record(ownerId, AuditAction.DELETE, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(), "Moved to trash");

        log.info("File moved to trash: id={}, fileId={}",
                saved.getId(), saved.getFileId());
    }

    /**
     * Restores a soft-deleted (trashed) file record by setting its status back
     * to {@code ACTIVE}.
     */
    @Override
    public FileResponse restoreFile(Long id, Long ownerId) {
        log.debug("Restoring soft-deleted file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findOwnedFile(id, ownerId);

        if (fileMetadata.getStatus() == FileStatus.ACTIVE) {
            log.warn("File is already active: id={}", id);
            throw new BadRequestException("File is already active — no restoration needed");
        }

        fileMetadata.setStatus(FileStatus.ACTIVE);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        auditLogService.record(ownerId, AuditAction.RESTORE, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(), "Restored from trash");

        log.info("File restored successfully: id={}, fileId={}",
                saved.getId(), saved.getFileId());

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Retrieves all soft-deleted (trashed) file metadata records for an owner.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getTrashFiles(Long ownerId) {
        log.debug("Fetching trash files for ownerId={}", ownerId);

        return fileMetadataRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED)
                .stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Permanently deletes a trashed file: removes the object from MinIO first,
     * then deletes the metadata row from MySQL (rollback-safe). Only files that
     * are currently in the trash can be permanently deleted.
     */
    @Override
    public void permanentlyDeleteFile(Long id, Long ownerId) {
        log.debug("Permanently deleting file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findOwnedFile(id, ownerId);

        if (fileMetadata.getStatus() != FileStatus.DELETED) {
            log.warn("File is not in the trash: id={}", id);
            throw new BadRequestException("Only files in the trash can be permanently deleted");
        }

        // ── 1. Remove the object from MinIO ────────────────────────────────────
        minioService.deleteObject(fileMetadata.getObjectName());

        // ── 2. Delete the metadata row from MySQL ──────────────────────────────
        fileMetadataRepository.delete(fileMetadata);

        auditLogService.record(ownerId, AuditAction.PERMANENT_DELETE, "FILE",
                String.valueOf(id), fileMetadata.getOriginalFileName(), "Permanently deleted");

        log.info("File permanently deleted: id={}, fileId={}, objectName={}",
                id, fileMetadata.getFileId(), fileMetadata.getObjectName());
    }

    /**
     * Permanently deletes every trashed file owned by the user (empty trash).
     * A single failure is logged and skipped so the rest of the trash is still
     * cleared.
     */
    @Override
    public void emptyTrash(Long ownerId) {
        log.debug("Emptying trash for ownerId={}", ownerId);

        List<FileMetadata> trashFiles =
                fileMetadataRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED);

        int deleted = 0;
        for (FileMetadata file : trashFiles) {
            try {
                minioService.deleteObject(file.getObjectName());
                fileMetadataRepository.delete(file);
                deleted++;
            } catch (RuntimeException e) {
                log.error("Failed to permanently delete file id={} while emptying trash: {}",
                        file.getId(), e.getMessage());
            }
        }

        auditLogService.record(ownerId, AuditAction.EMPTY_TRASH, "SYSTEM", null,
                null, "Emptied trash (" + deleted + " file(s))");

        log.info("Trash emptied: {} of {} file(s) permanently deleted for ownerId={}",
                deleted, trashFiles.size(), ownerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download & Preview
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams the file's binary content from MinIO for download.
     * Files flagged INFECTED (or still PENDING scan) are blocked.
     */
    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse downloadFile(Long id, Long ownerId) {
        log.debug("Downloading file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);
        assertDownloadable(fileMetadata, "download");

        InputStream content = minioService.getObject(fileMetadata.getObjectName());

        auditLogService.record(ownerId, AuditAction.DOWNLOAD, "FILE",
                String.valueOf(id), fileMetadata.getOriginalFileName(),
                fileMetadata.getFileSize() + " bytes");

        log.info("File download stream opened: id={}, fileId={}, objectName={}",
                id, fileMetadata.getFileId(), fileMetadata.getObjectName());

        return FileDownloadResponse.builder()
                .originalFileName(fileMetadata.getOriginalFileName())
                .contentType(fileMetadata.getContentType())
                .fileSize(fileMetadata.getFileSize())
                .inputStream(content)
                .build();
    }

    /**
     * Streams a file's content for a public share download.
     * <p>
     * The share token is validated against the Share Service (which enforces
     * expiry and password checks before ever reaching here) and must cover this
     * resource. No owner context is required — the token is the capability.
     * Infected or still-scanning files are still blocked.
     */
    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse downloadSharedFile(Long id, String token) {
        log.debug("Streaming shared file: id={}, token={}", id, token);

        if (token == null || token.isBlank()) {
            throw new ForbiddenException("A valid share token is required");
        }

        // ── Validate the share token against the Share Service ─────────────────
        ShareValidationResponse validation;
        try {
            StandardResponse<ShareValidationResponse> response =
                    shareServiceClient.validateShare(token, String.valueOf(id));
            validation = response != null ? response.getData() : null;
        } catch (Exception e) {
            log.error("Share Service validation failed for token: {}", e.getMessage());
            throw new FileStorageException(
                    "Unable to validate the share with the Share Service");
        }

        if (validation == null || !validation.isValid()) {
            log.warn("Share token rejected: id={}, token={}", id, token);
            throw new ForbiddenException(
                    "This share is not valid for the requested resource");
        }

        // ── Stream the content (no ownership check — the token is the grant) ──
        FileMetadata fileMetadata = findFileById(id);
        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            log.warn("Share download blocked: file {} is in the trash", id);
            throw new BadRequestException("This file has been deleted");
        }
        assertDownloadable(fileMetadata, "share download");

        InputStream content = minioService.getObject(fileMetadata.getObjectName());

        auditLogService.record(fileMetadata.getOwnerId(), AuditAction.SHARE_DOWNLOAD, "FILE",
                String.valueOf(id), fileMetadata.getOriginalFileName(),
                "Downloaded via public share link");

        log.info("Shared file download stream opened: id={}, fileId={}, token={}",
                id, fileMetadata.getFileId(), token);

        return FileDownloadResponse.builder()
                .originalFileName(fileMetadata.getOriginalFileName())
                .contentType(fileMetadata.getContentType())
                .fileSize(fileMetadata.getFileSize())
                .inputStream(content)
                .build();
    }

    /**
     * Streams the file's binary content from MinIO for in-browser preview.
     * Rejects content types that are not previewable, and files that are
     * infected or still being scanned.
     */
    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse previewFile(Long id, Long ownerId) {
        log.debug("Previewing file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);

        if (!isPreviewable(fileMetadata.getContentType())) {
            log.warn("Preview not supported for content type '{}' on file id={}",
                    fileMetadata.getContentType(), id);
            throw new BadRequestException(
                    "Preview is not supported for this file type: " + fileMetadata.getContentType());
        }

        assertDownloadable(fileMetadata, "preview");

        InputStream content = minioService.getObject(fileMetadata.getObjectName());

        auditLogService.record(ownerId, AuditAction.PREVIEW, "FILE",
                String.valueOf(id), fileMetadata.getOriginalFileName(), "Previewed");

        log.info("File preview stream opened: id={}, fileId={}, objectName={}",
                id, fileMetadata.getFileId(), fileMetadata.getObjectName());

        return FileDownloadResponse.builder()
                .originalFileName(fileMetadata.getOriginalFileName())
                .contentType(fileMetadata.getContentType())
                .fileSize(fileMetadata.getFileSize())
                .inputStream(content)
                .build();
    }

    /**
     * Blocks access to files whose content is infected or still being scanned.
     */
    private void assertDownloadable(FileMetadata fileMetadata, String operation) {
        if (fileMetadata.getScanStatus() == ScanStatus.INFECTED) {
            log.warn("Blocked {} of infected file id={}", operation, fileMetadata.getId());
            throw new ForbiddenException(
                    "This file is blocked — a virus was detected in its content");
        }
        if (fileMetadata.getScanStatus() == ScanStatus.PENDING
                || fileMetadata.getScanStatus() == ScanStatus.SCANNING) {
            log.warn("Blocked {} of file id={} — scan in progress", operation, fileMetadata.getId());
            throw new BadRequestException(
                    "This file is still being scanned — try again shortly");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Favorites
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all active file metadata records marked as favorite by an owner.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getFavoriteFiles(Long ownerId) {
        log.debug("Fetching favorite files for ownerId={}", ownerId);

        return fileMetadataRepository.findFavoritesByOwnerId(ownerId)
                .stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Marks (or unmarks) a file as favorite. When {@code favorite} is
     * {@code null}, the current value is toggled.
     */
    @Override
    public FileResponse setFavorite(Long id, Boolean favorite, Long ownerId) {
        log.debug("Setting favorite: id={}, favorite={}, ownerId={}", id, favorite, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);
        boolean target = favorite != null ? favorite : !fileMetadata.getIsFavorite();

        fileMetadata.setIsFavorite(target);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

        auditLogService.record(ownerId,
                target ? AuditAction.FAVORITE_ADD : AuditAction.FAVORITE_REMOVE,
                "FILE", String.valueOf(saved.getId()), saved.getOriginalFileName(), null);

        log.info("Favorite updated: id={}, fileId={}, isFavorite={}",
                saved.getId(), saved.getFileId(), saved.getIsFavorite());

        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────────

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

        List<FileMetadata> results = fileMetadataRepository.searchByFileName(query.trim(), ownerId);

        log.debug("Search found {} files for query='{}'", results.size(), query);

        return results.stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2 — bulk download, scan status, analytics, audit logs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams a ZIP archive of the selected files / folders.
     */
    @Override
    @Transactional(readOnly = true)
    public void downloadZip(DownloadZipRequest request, OutputStream out, Long ownerId) {
        zipDownloadService.streamZip(request, out, ownerId);
    }

    /**
     * Returns the current virus-scan status of a file.
     */
    @Override
    @Transactional(readOnly = true)
    public ScanStatusResponse getScanStatus(Long id, Long ownerId) {
        FileMetadata fileMetadata = findOwnedFile(id, ownerId);
        return ScanStatusResponse.builder()
                .fileId(fileMetadata.getFileId())
                .scanStatus(fileMetadata.getScanStatus() != null
                        ? fileMetadata.getScanStatus()
                        : ScanStatus.CLEAN)
                .build();
    }

    /**
     * Computes the storage analytics overview for the user.
     */
    @Override
    @Transactional(readOnly = true)
    public StorageOverviewResponse getStorageOverview(Long ownerId) {
        return storageAnalyticsService.getOverview(ownerId);
    }

    /**
     * Returns a page of the user's audit-trail entries.
     */
    @Override
    @Transactional(readOnly = true)
    public PagedAuditLogsResponse getAuditLogs(Long ownerId, int page, int size, String action) {
        return auditLogService.getLogs(ownerId, page, size, action);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private DuplicateFileInfo toDuplicateInfo(FileMetadata file) {
        return DuplicateFileInfo.builder()
                .id(file.getId())
                .fileId(file.getFileId())
                .originalFileName(file.getOriginalFileName())
                .fileSize(file.getFileSize())
                .checksum(file.getChecksum())
                .folderId(file.getFolderId())
                .uploadedAt(file.getUploadedAt() != null ? file.getUploadedAt() : file.getCreatedAt())
                .build();
    }

    private String shortChecksum(String checksum) {
        return checksum != null && checksum.length() > 8 ? checksum.substring(0, 8) : checksum;
    }

    /**
     * Rejects requests that carry no user identity (401).
     */
    private void validateOwner(Long ownerId) {
        if (ownerId == null) {
            throw new UnauthorizedException("Missing user identity — unable to determine file owner");
        }
    }

    /**
     * Rejects empty and oversized files.
     */
    private void validateFileForUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file");
        }

        if (file.getSize() > minioProperties.getMaxFileSize()) {
            throw new FileTooLargeException(
                    "File size exceeds the maximum allowed size of "
                            + minioProperties.getMaxFileSize() + " bytes");
        }
    }

    /**
     * Validates that the referenced folder exists and belongs to the owner.
     * <p>
     * The Folder Service enforces ownership itself (returns 404 when the folder
     * is missing, deleted, or owned by someone else).
     *
     * @param folderId the folder UUID, or {@code null} to skip validation
     * @param ownerId  the authenticated user's ID
     */
    private void validateFolder(String folderId, Long ownerId) {
        if (folderId == null || folderId.isBlank()) {
            return; // root-level placement — nothing to validate
        }

        try {
            StandardResponse<FolderResponse> response =
                    folderServiceClient.getFolderById(folderId, ownerId);

            if (response == null || response.getData() == null) {
                log.warn("Folder validation failed: no data for folderId={}", folderId);
                throw new ResourceNotFoundException("Folder not found with id: " + folderId);
            }
            log.debug("Folder validated: id={}, name='{}'", folderId, response.getData().getName());
        } catch (FeignException.NotFound e) {
            log.warn("Folder not found: folderId={}, ownerId={}", folderId, ownerId);
            throw new ResourceNotFoundException("Folder not found with id: " + folderId);
        } catch (FeignException e) {
            log.error("Folder Service error while validating folderId={}: {}",
                    folderId, e.getMessage());
            throw new FileStorageException("Unable to validate the target folder with the Folder Service");
        } catch (RuntimeException e) {
            if (e instanceof ResourceNotFoundException || e instanceof FileStorageException) {
                throw e;
            }
            log.error("Unexpected error while validating folderId={}: {}", folderId, e.getMessage());
            throw new FileStorageException("Unable to validate the target folder with the Folder Service");
        }
    }

    /**
     * Resolves the content type, falling back to a default when absent.
     */
    private String resolveContentType(String contentType) {
        return (contentType == null || contentType.isBlank())
                ? DEFAULT_CONTENT_TYPE
                : contentType;
    }

    /**
     * Returns whether the given MIME type supports in-browser preview.
     */
    private boolean isPreviewable(String contentType) {
        if (contentType == null) {
            return false;
        }
        return PREVIEWABLE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT));
    }

    /**
     * Enforces ownership only when an owner context is supplied.
     *
     * @param fileMetadata the file metadata record
     * @param ownerId      the authenticated user's ID, or {@code null} for internal access
     * @throws ForbiddenException if an owner context is present and does not match
     */
    private void assertOwner(FileMetadata fileMetadata, Long ownerId) {
        if (ownerId != null && !fileMetadata.getOwnerId().equals(ownerId)) {
            log.warn("User {} attempted to access file {} owned by {}",
                    ownerId, fileMetadata.getId(), fileMetadata.getOwnerId());
            throw new ForbiddenException("You do not have access to this file");
        }
    }

    /**
     * Finds a file metadata record by ID, enforcing ownership.
     *
     * @param id      the internal primary key
     * @param ownerId the authenticated user's ID
     * @return the owned FileMetadata entity
     * @throws ResourceNotFoundException if no record exists with the given ID
     * @throws ForbiddenException         if the record belongs to another user
     */
    private FileMetadata findOwnedFile(Long id, Long ownerId) {
        FileMetadata fileMetadata = findFileById(id);
        assertOwner(fileMetadata, ownerId);
        return fileMetadata;
    }

    /**
     * Finds an active (non-soft-deleted) file metadata record by ID,
     * enforcing ownership.
     */
    private FileMetadata findActiveOwnedFile(Long id, Long ownerId) {
        FileMetadata fileMetadata = findOwnedFile(id, ownerId);

        if (fileMetadata.getStatus() == FileStatus.DELETED) {
            throw new BadRequestException("This file has been deleted");
        }
        return fileMetadata;
    }

    /**
     * Internal helper to find a file metadata record by ID or throw
     * {@link ResourceNotFoundException}.
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
