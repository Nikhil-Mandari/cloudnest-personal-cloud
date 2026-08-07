package com.cloudnest.file.service.impl;

import com.cloudnest.file.config.MinioProperties;
import com.cloudnest.file.dto.FileDownloadResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.FileVersionResponse;
import com.cloudnest.file.entity.AuditLog.AuditAction;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.entity.FileVersion;
import com.cloudnest.file.entity.ScanStatus;
import com.cloudnest.file.exception.BadRequestException;
import com.cloudnest.file.exception.FileStorageException;
import com.cloudnest.file.exception.FileTooLargeException;
import com.cloudnest.file.exception.ForbiddenException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.mapper.FileMapper;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.repository.FileVersionRepository;
import com.cloudnest.file.service.AuditLogService;
import com.cloudnest.file.service.MinioService;
import com.cloudnest.file.service.VersionService;
import com.cloudnest.file.service.VirusScanService;
import com.cloudnest.file.util.ChecksumUtil;
import com.cloudnest.file.util.FileNameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Implementation of {@link VersionService}.
 */
@Slf4j
@Service
@Transactional
public class VersionServiceImpl implements VersionService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final FileVersionRepository versionRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;
    private final MinioProperties minioProperties;
    private final FileMapper fileMapper;
    private final VirusScanService virusScanService;
    private final AuditLogService auditLogService;

    public VersionServiceImpl(
            FileVersionRepository versionRepository,
            FileMetadataRepository fileMetadataRepository,
            MinioService minioService,
            MinioProperties minioProperties,
            FileMapper fileMapper,
            VirusScanService virusScanService,
            AuditLogService auditLogService) {
        this.versionRepository = versionRepository;
        this.fileMetadataRepository = fileMetadataRepository;
        this.minioService = minioService;
        this.minioProperties = minioProperties;
        this.fileMapper = fileMapper;
        this.virusScanService = virusScanService;
        this.auditLogService = auditLogService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<FileVersionResponse> getVersions(Long fileId, Long ownerId) {
        FileMetadata file = findActiveOwnedFile(fileId, ownerId);
        return versionRepository.findByFileMetadataIdOrderByVersionNumberDesc(file.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload new version
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public FileResponse uploadNewVersion(Long fileId, MultipartFile file, Long ownerId) {
        FileMetadata metadata = findActiveOwnedFile(fileId, ownerId);

        validateFileForUpload(file);

        // ── Derive storage metadata ───────────────────────────────────────────
        String contentType = (file.getContentType() == null || file.getContentType().isBlank())
                ? DEFAULT_CONTENT_TYPE
                : file.getContentType();
        String objectName = FileNameUtil.generateObjectName(metadata.getOriginalFileName());

        // ── Checksum ──────────────────────────────────────────────────────────
        String checksum;
        try (InputStream checksumStream = file.getInputStream()) {
            checksum = ChecksumUtil.sha256Hex(checksumStream);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }

        // ── Upload to MinIO ───────────────────────────────────────────────────
        try (InputStream uploadStream = file.getInputStream()) {
            minioService.uploadObject(objectName, uploadStream, file.getSize(), contentType);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }

        // ── Virus scan (roll back the object if infected) ─────────────────────
        ScanStatus scanStatus;
        try (InputStream scanStream = file.getInputStream()) {
            scanStatus = virusScanService.scan(scanStream, objectName);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read uploaded file content", e);
        }
        if (scanStatus == ScanStatus.INFECTED) {
            minioService.deleteObject(objectName);
            throw new com.cloudnest.file.exception.VirusDetectedException(
                    "A virus was detected in the uploaded content — the file was blocked");
        }

        // ── Archive the current content, then swap the file to the new one ────
        archiveCurrent(metadata, ownerId);

        metadata.setObjectName(objectName);
        metadata.setStoredFileName(objectName);
        metadata.setChecksum(checksum);
        metadata.setFileSize(file.getSize());
        metadata.setContentType(contentType);
        metadata.setFileType(contentType);
        metadata.setUploadedAt(LocalDateTime.now());
        metadata.setScanStatus(scanStatus);

        FileMetadata saved = fileMetadataRepository.save(metadata);

        auditLogService.record(ownerId, AuditAction.VERSION_UPLOAD, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(),
                "New version v" + nextVersionNumber(saved.getId()) + " · " + saved.getFileSize() + " bytes");

        log.info("New version uploaded: fileId={}, objectName={}, size={}", fileId, objectName, file.getSize());
        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Restore
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public FileResponse restoreVersion(Long fileId, Long versionId, Long ownerId) {
        FileMetadata metadata = findActiveOwnedFile(fileId, ownerId);
        FileVersion version = findVersion(versionId, metadata.getId());

        // Restoring the version that already is the current content = no-op.
        if (metadata.getObjectName().equals(version.getObjectName())) {
            log.debug("Version {} is already the current content of file {}", versionId, fileId);
            return fileMapper.toFileResponse(metadata);
        }

        // Archive the current content so nothing is lost.
        archiveCurrent(metadata, ownerId);

        metadata.setObjectName(version.getObjectName());
        metadata.setStoredFileName(version.getObjectName());
        metadata.setChecksum(version.getChecksum());
        metadata.setFileSize(version.getFileSize());
        metadata.setContentType(version.getContentType());
        metadata.setFileType(version.getContentType());
        metadata.setUploadedAt(version.getCreatedAt());
        metadata.setScanStatus(ScanStatus.CLEAN);

        FileMetadata saved = fileMetadataRepository.save(metadata);

        auditLogService.record(ownerId, AuditAction.VERSION_RESTORE, "FILE",
                String.valueOf(saved.getId()), saved.getOriginalFileName(),
                "Restored version v" + version.getVersionNumber());

        log.info("Version {} restored for file {}", versionId, fileId);
        return fileMapper.toFileResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void deleteVersion(Long fileId, Long versionId, Long ownerId) {
        FileMetadata metadata = findOwnedFile(fileId, ownerId);
        FileVersion version = findVersion(versionId, metadata.getId());

        if (metadata.getObjectName().equals(version.getObjectName())) {
            throw new BadRequestException(
                    "This version holds the file's current content and cannot be deleted");
        }

        // Guard: a restore-then-replace flow can create two version rows that
        // point at the same object. Never delete an object still referenced by
        // another version row — only remove this row's reference.
        boolean referencedElsewhere = versionRepository.existsByObjectNameAndIdNot(
                version.getObjectName(), version.getId());
        if (referencedElsewhere) {
            log.info("Version {} deletion: object '{}' still referenced by another version — "
                    + "skipping MinIO deletion", version.getId(), version.getObjectName());
        } else {
            minioService.deleteObject(version.getObjectName());
        }

        versionRepository.delete(version);

        auditLogService.record(ownerId, AuditAction.VERSION_DELETE, "VERSION",
                String.valueOf(version.getId()), metadata.getOriginalFileName(),
                "Deleted version v" + version.getVersionNumber());

        log.info("Version {} deleted for file {}", versionId, fileId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Download
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public FileDownloadResponse downloadVersion(Long fileId, Long versionId, Long ownerId) {
        FileMetadata metadata = findActiveOwnedFile(fileId, ownerId);
        FileVersion version = findVersion(versionId, metadata.getId());

        InputStream content = minioService.getObject(version.getObjectName());

        auditLogService.record(ownerId, AuditAction.DOWNLOAD, "VERSION",
                String.valueOf(version.getId()), metadata.getOriginalFileName(),
                "Downloaded version v" + version.getVersionNumber());

        return FileDownloadResponse.builder()
                .originalFileName(metadata.getOriginalFileName())
                .contentType(version.getContentType())
                .fileSize(version.getFileSize())
                .inputStream(content)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Archive
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void archiveCurrent(FileMetadata file, Long ownerId) {
        if (file.getObjectName() == null || file.getObjectName().isBlank()) {
            log.warn("Skipping archive for file {} — no object name set", file.getId());
            return;
        }

        int next = nextVersionNumber(file.getId());
        FileVersion version = FileVersion.builder()
                .fileMetadataId(file.getId())
                .versionNumber(next)
                .objectName(file.getObjectName())
                .storedFileName(file.getObjectName())
                .fileSize(file.getFileSize())
                .checksum(file.getChecksum())
                .contentType(file.getContentType())
                .uploadedBy(ownerId)
                .build();

        versionRepository.save(version);
        log.debug("Archived current content of file {} as version {}", file.getId(), next);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int nextVersionNumber(Long fileMetadataId) {
        return versionRepository.findMaxVersionNumberByFileMetadataId(fileMetadataId).orElse(0) + 1;
    }

    private FileVersionResponse toResponse(FileVersion version) {
        return FileVersionResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .fileSize(version.getFileSize())
                .contentType(version.getContentType())
                .checksum(version.getChecksum())
                .uploadedBy(version.getUploadedBy())
                .note(version.getNote())
                .createdAt(version.getCreatedAt())
                .build();
    }

    private FileVersion findVersion(Long versionId, Long fileMetadataId) {
        return versionRepository.findByIdAndFileMetadataId(versionId, fileMetadataId)
                .orElseThrow(() -> {
                    log.warn("Version not found: versionId={}, fileMetadataId={}",
                            versionId, fileMetadataId);
                    return new ResourceNotFoundException(
                            "Version not found with id: " + versionId);
                });
    }

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

    private FileMetadata findOwnedFile(Long fileId, Long ownerId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        if (!metadata.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You do not have access to this file");
        }
        return metadata;
    }

    private FileMetadata findActiveOwnedFile(Long fileId, Long ownerId) {
        FileMetadata metadata = findOwnedFile(fileId, ownerId);
        if (metadata.getStatus() == FileStatus.DELETED) {
            throw new BadRequestException("This file has been deleted");
        }
        return metadata;
    }
}
