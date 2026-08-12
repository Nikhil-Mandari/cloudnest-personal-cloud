package com.cloudnest.file.service.impl;

import com.cloudnest.file.client.FolderServiceClient;
import com.cloudnest.file.config.MinioProperties;
import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.FolderResponse;
import com.cloudnest.file.dto.StorageOverviewResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.exception.BadRequestException;
import com.cloudnest.file.exception.DuplicateResourceException;
import com.cloudnest.file.exception.FileStorageException;
import com.cloudnest.file.exception.FileTooLargeException;
import com.cloudnest.file.exception.ForbiddenException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.exception.UnauthorizedException;
import com.cloudnest.file.mapper.FileMapper;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.service.FileService;
import com.cloudnest.file.service.MinioService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link FileService} interface.
 * <p>
 * Orchestrates the MinIO-backed file lifecycle: binary content is uploaded to /
 * read from MinIO object storage, while metadata (object key, bucket, content
 * type, size, SHA-256 checksum) is persisted in MySQL. Every mutation is
 * ownership-checked against the authenticated user.
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
            // Modern browsers render these directly; without them an uploaded
            // WebP / AVIF / BMP / SVG / TIFF image shows "Preview not available"
            // even though the frontend treats every image/* as previewable.
            "image/webp",
            "image/avif",
            "image/bmp",
            "image/svg+xml",
            "image/tiff",
            "image/x-tiff",
            "image/x-icon",
            "image/heic",
            "image/heif",
            "text/plain",
            "text/markdown"
    );

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMapper fileMapper;
    private final MinioService minioService;
    private final MinioProperties minioProperties;
    private final FolderServiceClient folderServiceClient;

    public FileServiceImpl(
            FileMetadataRepository fileMetadataRepository,
            FileMapper fileMapper,
            MinioService minioService,
            MinioProperties minioProperties,
            FolderServiceClient folderServiceClient) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileMapper = fileMapper;
        this.minioService = minioService;
        this.minioProperties = minioProperties;
        this.folderServiceClient = folderServiceClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates the upload, computes the SHA-256 checksum, uploads the binary
     * content to MinIO, then persists the metadata in MySQL.
     * <p>
     * If persisting the metadata fails, the just-uploaded object is removed
     * from MinIO (rollback) so no orphaned objects remain.
     */
    @Override
    public FileResponse uploadFile(UploadFileRequest request, MultipartFile file) {
        log.debug("Uploading file: originalFileName='{}', ownerId={}, folderId={}",
                request.getOriginalFileName(), request.getOwnerId(), request.getFolderId());

        // ── Validate the request ─────────────────────────────────────────────
        validateOwner(request.getOwnerId());
        validateFileForUpload(file);
        validateFolder(request.getFolderId(), request.getOwnerId());

        // ── Derive storage metadata ───────────────────────────────────────────
        String originalFileName = FileNameUtil.sanitizeFileName(request.getOriginalFileName());
        String contentType = resolveContentType(request.getContentType());
        String objectName = FileNameUtil.generateObjectName(originalFileName);

        // ── Compute SHA-256 checksum while the content is available ───────────
        String checksum;
        try (InputStream checksumStream = file.getInputStream()) {
            checksum = ChecksumUtil.sha256Hex(checksumStream);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }
        request.setChecksum(checksum);

        // ── Upload binary content to MinIO ────────────────────────────────────
        try (InputStream uploadStream = file.getInputStream()) {
            minioService.uploadObject(objectName, uploadStream, file.getSize(), contentType);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }

        // ── Persist metadata into MySQL (rollback object on failure) ──────────
        FileMetadata metadata = fileMapper.toEntity(request);
        metadata.setFileId(UUID.randomUUID().toString());
        metadata.setObjectName(objectName);
        metadata.setStoredFileName(objectName);
        metadata.setBucketName(minioProperties.getBucketName());
        metadata.setStoragePath(minioProperties.getBucketName() + "/" + objectName);
        metadata.setUploadedAt(LocalDateTime.now());

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

        log.info("File uploaded successfully: id={}, fileId={}, objectName={}, size={}, checksum={}",
                saved.getId(), saved.getFileId(), saved.getObjectName(), saved.getFileSize(),
                saved.getChecksum());

        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all active file metadata records for a specific owner.
     * Filters out soft-deleted (legacy) records.
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
     * Retrieves the active file metadata records for a specific owner, scoped
     * to one explorer location.
     * <p>
     * The explorer (Files / Folders pages) always sends a {@code folderId}
     * hint: absent = global "all files" view, blank = root level, UUID = a
     * specific folder. Without this scoping every folder view would show the
     * user's complete file set, which surfaces as files appearing in the wrong
     * location (and duplicated across views) after uploads and moves.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getUserFiles(Long ownerId, String folderId) {
        log.debug("Fetching files for ownerId={}, folderId={}", ownerId, folderId);

        if (folderId == null) {
            // Dashboard / global view — every active file.
            return getUserFiles(ownerId);
        }

        List<FileMetadata> files = folderId.isBlank()
                ? fileMetadataRepository.findRootFilesByOwnerId(ownerId)
                : fileMetadataRepository.findByOwnerIdAndFolderIdAndStatus(ownerId, folderId);

        return files.stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed file metadata by its internal ID.
     * <p>
     * Ownership is always enforced: external callers arrive through the API
     * Gateway with the authenticated user's ID, and internal Feign consumers
     * (Share Service) forward the resource owner's ID.
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

        // ── Apply partial update via MapStruct ──────────────────────────────────
        fileMapper.applyUpdate(fileMetadata, request);
        FileMetadata saved = fileMetadataRepository.save(fileMetadata);

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

        log.info("File moved successfully: id={}, fileId={}, newFolderId={}",
                saved.getId(), saved.getFileId(), newFolderId);

        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete (soft delete: move to trash)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a file by moving it to the trash: the metadata status is set
     * to {@code DELETED} and the MinIO object is retained so the file can be
     * restored from the trash.
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

        log.info("File restored successfully: id={}, fileId={}",
                saved.getId(), saved.getFileId());

        return fileMapper.toFileResponse(saved);
    }

    /**
     * Retrieves all soft-deleted (trashed) file metadata records for an owner.
     * Only records with {@code DELETED} status are returned.
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> getTrashFiles(Long ownerId) {
        log.debug("Fetching trashed files for ownerId={}", ownerId);

        return fileMetadataRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED)
                .stream()
                .map(fileMapper::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /**
     * Permanently deletes a trashed file: removes the object from MinIO first,
     * then deletes the metadata row from MySQL. Only files that are currently
     * in the trash can be permanently deleted.
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

        log.info("Trash emptied: {} of {} file(s) permanently deleted for ownerId={}",
                deleted, trashFiles.size(), ownerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download & Preview
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Streams the file's binary content from MinIO for download.
     */
    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse downloadFile(Long id, Long ownerId) {
        log.debug("Downloading file: id={}, ownerId={}", id, ownerId);

        FileMetadata fileMetadata = findActiveOwnedFile(id, ownerId);
        InputStream content = minioService.getObject(fileMetadata.getObjectName());

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
     * Streams the file's binary content from MinIO for in-browser preview.
     * Rejects content types that are not previewable.
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

        InputStream content = minioService.getObject(fileMetadata.getObjectName());

        log.info("File preview stream opened: id={}, fileId={}, objectName={}",
                id, fileMetadata.getFileId(), fileMetadata.getObjectName());

        return FileDownloadResponse.builder()
                .originalFileName(fileMetadata.getOriginalFileName())
                .contentType(fileMetadata.getContentType())
                .fileSize(fileMetadata.getFileSize())
                .inputStream(content)
                .build();
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
    // Storage analytics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the storage analytics overview for a user from the metadata in
     * MySQL (folder count is delegated to the Folder Service via Feign).
     */
    @Override
    @Transactional(readOnly = true)
    public StorageOverviewResponse getStorageOverview(Long ownerId) {
        log.debug("Building storage overview for ownerId={}", ownerId);

        validateOwner(ownerId);

        List<FileMetadata> active = fileMetadataRepository.findByOwnerId(ownerId).stream()
                .filter(file -> file.getStatus() == FileStatus.ACTIVE)
                .collect(Collectors.toList());
        List<FileMetadata> trash = fileMetadataRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED);

        long storageUsed = active.stream().mapToLong(FileMetadata::getFileSize).sum();
        long trashSize = trash.stream().mapToLong(FileMetadata::getFileSize).sum();

        List<StorageOverviewResponse.LargestFileInfo> largestFiles = active.stream()
                .sorted(Comparator.comparing(FileMetadata::getFileSize).reversed())
                .limit(5)
                .map(file -> StorageOverviewResponse.LargestFileInfo.builder()
                        .id(file.getId())
                        .originalFileName(file.getOriginalFileName())
                        .fileSize(file.getFileSize())
                        // Category key (image/video/pdf/…) so the frontend can pick
                        // the right icon without re-mapping the raw MIME type.
                        .fileType(categorize(file.getContentType()))
                        .folderId(file.getFolderId())
                        .uploadedAt(file.getUploadedAt() != null ? file.getUploadedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());

        List<StorageOverviewResponse.FileTypeStat> fileTypeStats = buildFileTypeStats(active);

        return StorageOverviewResponse.builder()
                .storageUsed(storageUsed)
                .fileCount(active.size())
                .folderCount(fetchFolderCount(ownerId))
                .trashFileCount(trash.size())
                .trashSize(trashSize)
                .largestFiles(largestFiles)
                .fileTypeStats(fileTypeStats)
                .weeklyUsage(buildTimeline(active, 7, true))
                .monthlyUsage(buildTimeline(active, 6, false))
                .build();
    }

    /**
     * Groups active files by file-type category (mirrors the frontend buckets).
     */
    private List<StorageOverviewResponse.FileTypeStat> buildFileTypeStats(List<FileMetadata> files) {
        Map<String, long[]> byCategory = new LinkedHashMap<>();
        for (FileMetadata file : files) {
            String category = categorize(file.getContentType());
            long[] totals = byCategory.computeIfAbsent(category, k -> new long[2]);
            totals[0] += file.getFileSize();
            totals[1] += 1;
        }
        return byCategory.entrySet().stream()
                .map(entry -> StorageOverviewResponse.FileTypeStat.builder()
                        .category(entry.getKey())
                        .bytes(entry.getValue()[0])
                        .count(entry.getValue()[1])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Builds a usage timeline (daily buckets for a week, monthly buckets for
     * half a year) from the upload timestamps of active files.
     */
    private List<StorageOverviewResponse.UsagePoint> buildTimeline(
            List<FileMetadata> files, int buckets, boolean daily) {
        LocalDate today = LocalDate.now();
        List<StorageOverviewResponse.UsagePoint> points = new ArrayList<>(buckets);
        for (int offset = buckets - 1; offset >= 0; offset--) {
            LocalDate bucketStart = daily ? today.minusDays(offset) : today.minusMonths(offset);
            LocalDate bucketEnd = bucketStart.plusDays(1);
            String label = daily
                    ? bucketStart.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    : bucketStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            long bytes = 0L;
            for (FileMetadata file : files) {
                LocalDateTime uploaded = file.getUploadedAt();
                if (uploaded == null) {
                    continue;
                }
                LocalDate date = uploaded.toLocalDate();
                boolean inBucket = daily
                        ? (!date.isBefore(bucketStart) && date.isBefore(bucketEnd))
                        : (date.getYear() == bucketStart.getYear()
                            && date.getMonth() == bucketStart.getMonth());
                if (inBucket) {
                    bytes += file.getFileSize();
                }
            }
            points.add(StorageOverviewResponse.UsagePoint.builder()
                    .label(label)
                    .bytes(bytes)
                    .build());
        }
        return points;
    }

    /**
     * Maps a MIME content type to a display category used by the analytics UI.
     */
    private String categorize(String contentType) {
        if (contentType == null) {
            return "other";
        }
        String type = contentType.toLowerCase(Locale.ROOT);
        if (type.startsWith("image/")) {
            return "image";
        }
        if (type.startsWith("video/")) {
            return "video";
        }
        if (type.startsWith("audio/")) {
            return "audio";
        }
        if (type.equals("application/pdf")) {
            return "pdf";
        }
        if (type.startsWith("application/zip")
                || type.contains("gzip") || type.contains("tar")
                || type.contains("rar") || type.contains("7z")
                || type.contains("compress")) {
            return "archive";
        }
        if (type.contains("json") || type.contains("xml")
                || type.contains("javascript") || type.contains("yaml")
                || type.contains("graphql") || type.contains("typescript")) {
            return "code";
        }
        if (type.startsWith("text/")) {
            return "document";
        }
        return "other";
    }

    /**
     * Delegates the folder count to the Folder Service (best effort — the
     * analytics endpoint must never fail because the folder count is missing).
     */
    private long fetchFolderCount(Long ownerId) {
        try {
            StandardResponse<List<FolderResponse>> response =
                    folderServiceClient.getAllFolders(ownerId);
            if (response == null || response.getData() == null) {
                return 0L;
            }
            return response.getData().size();
        } catch (Exception e) {
            log.debug("Could not resolve folder count for ownerId={}: {}", ownerId, e.getMessage());
            return 0L;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

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
