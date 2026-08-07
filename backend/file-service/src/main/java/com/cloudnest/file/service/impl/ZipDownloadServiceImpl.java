package com.cloudnest.file.service.impl;

import com.cloudnest.file.client.FolderServiceClient;
import com.cloudnest.file.dto.DownloadZipRequest;
import com.cloudnest.file.dto.FolderResponse;
import com.cloudnest.file.entity.AuditLog.AuditAction;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.exception.BadRequestException;
import com.cloudnest.file.exception.FileStorageException;
import com.cloudnest.file.exception.ForbiddenException;
import com.cloudnest.file.exception.ResourceNotFoundException;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.service.AuditLogService;
import com.cloudnest.file.service.MinioService;
import com.cloudnest.file.service.ZipDownloadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of {@link ZipDownloadService}.
 * <p>
 * Resolves the folder tree through the Folder Service (each folder carries its
 * full hierarchical {@code path}), collects every selected file, and streams
 * a ZIP where each entry lives under {@code CloudNest/<folder path>/<file>}.
 * All entry paths are sanitised to prevent ZIP-slip traversal.
 */
@Slf4j
@Service
public class ZipDownloadServiceImpl implements ZipDownloadService {

    private static final String ROOT_DIR = "CloudNest";
    private static final int MAX_ENTRIES = 10_000;
    private static final int BUFFER_SIZE = 16_384;

    private final FileMetadataRepository fileMetadataRepository;
    private final MinioService minioService;
    private final FolderServiceClient folderServiceClient;
    private final AuditLogService auditLogService;

    public ZipDownloadServiceImpl(
            FileMetadataRepository fileMetadataRepository,
            MinioService minioService,
            FolderServiceClient folderServiceClient,
            AuditLogService auditLogService) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.minioService = minioService;
        this.folderServiceClient = folderServiceClient;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public void streamZip(DownloadZipRequest request, OutputStream out, Long ownerId) {
        List<Long> fileIds = request.safeFileIds();
        List<String> folderIds = request.safeFolderIds();

        if (fileIds.isEmpty() && folderIds.isEmpty()) {
            throw new BadRequestException("Select at least one file or folder to download");
        }

        // ── Resolve the folder tree (fails open to an empty tree) ─────────────
        Map<String, FolderResponse> foldersById = resolveFolderTree(ownerId);

        // ── Collect every selected folder and its descendants ─────────────────
        Set<String> selectedFolderIds = new HashSet<>();
        Map<String, List<String>> childrenByParent = buildChildrenIndex(foldersById);
        for (String folderId : folderIds) {
            if (!foldersById.containsKey(folderId)) {
                throw new ResourceNotFoundException("Folder not found with id: " + folderId);
            }
            collectDescendants(folderId, foldersById, childrenByParent, selectedFolderIds);
        }

        // ── Assemble entries ───────────────────────────────────────────────────
        List<Entry> entries = new ArrayList<>();

        // Explicit files → CloudNest/<name>
        for (Long fileId : fileIds) {
            FileMetadata file = findActiveOwnedFile(fileId, ownerId);
            entries.add(new Entry(joinPath(ROOT_DIR, sanitize(file.getOriginalFileName())), file));
        }

        // Folder files → CloudNest/<folder path>/<name>
        for (String folderId : selectedFolderIds) {
            FolderResponse folder = foldersById.get(folderId);
            String base = folderEntryBase(folder);
            List<FileMetadata> files = fileMetadataRepository.findActiveByOwnerIdAndFolderId(ownerId, folderId);
            for (FileMetadata file : files) {
                entries.add(new Entry(joinPath(base, sanitize(file.getOriginalFileName())), file));
            }
        }

        if (entries.isEmpty()) {
            throw new BadRequestException("Nothing to download — the selection is empty");
        }
        if (entries.size() > MAX_ENTRIES) {
            throw new BadRequestException(
                    "The selection contains more than " + MAX_ENTRIES + " files");
        }

        // ── Stream the archive ─────────────────────────────────────────────────
        int written;
        try {
            written = writeArchive(out, entries);
        } catch (IOException e) {
            log.error("Failed to stream ZIP archive for ownerId={}: {}", ownerId, e.getMessage());
            throw new FileStorageException("Failed to generate the ZIP archive", e);
        }

        auditLogService.record(ownerId, AuditAction.ZIP_DOWNLOAD, "SYSTEM", null,
                "Bulk download", written + " file(s) in " + entries.size() + " selection(s)");

        log.info("ZIP download streamed for ownerId={}: {} entries", ownerId, entries.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int writeArchive(OutputStream out, List<Entry> entries) throws IOException {
        Set<String> createdDirs = new HashSet<>();
        int written = 0;
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                ensureParentDirs(zos, createdDirs, entry.path());
                zos.putNextEntry(new ZipEntry(entry.path()));
                try (InputStream in = minioService.getObject(entry.file().getObjectName())) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        zos.write(buffer, 0, read);
                    }
                }
                zos.closeEntry();
                written++;
            }
            zos.finish();
        }
        return written;
    }

    private void ensureParentDirs(ZipOutputStream zos, Set<String> createdDirs, String entryPath) {
        int lastSlash = entryPath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return;
        }
        String parent = entryPath.substring(0, lastSlash);
        if (createdDirs.add(parent)) {
            try {
                zos.putNextEntry(new ZipEntry(parent + "/"));
                zos.closeEntry();
            } catch (IOException e) {
                log.warn("Could not write directory entry '{}': {}", parent, e.getMessage());
            }
        }
    }

    /**
     * Folder base path inside the archive, e.g. {@code CloudNest/Documents/Work}.
     * Uses the folder's hierarchical {@code path} when available; otherwise the
     * folder name. Path segments are sanitised and traversal is stripped.
     */
    private String folderEntryBase(FolderResponse folder) {
        String raw = folder.getPath();
        if (raw == null || raw.isBlank()) {
            raw = "/" + folder.getName();
        }
        String cleaned = sanitizePath(raw);
        return joinPath(ROOT_DIR, cleaned);
    }

    /**
     * Sanitises a full relative path: converts separators, strips traversal
     * segments and leading slashes.
     */
    private String sanitizePath(String path) {
        String[] segments = path.split("[/\\\\]");
        List<String> safe = new ArrayList<>();
        for (String segment : segments) {
            String cleaned = sanitize(segment);
            if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) {
                continue;
            }
            safe.add(cleaned);
        }
        return String.join("/", safe);
    }

    /**
     * Sanitises a single path segment: strips path separators, traversal and
     * control characters.
     */
    private String sanitize(String segment) {
        if (segment == null) {
            return "file";
        }
        String cleaned = segment
                .replace('\\', '_')
                .replace('/', '_')
                .replace("\u0000", "")
                .trim();
        if (cleaned.isEmpty() || cleaned.equals(".") || cleaned.equals("..")) {
            return "file";
        }
        return cleaned;
    }

    private String joinPath(String base, String child) {
        if (base == null || base.isBlank()) {
            return child;
        }
        return base + "/" + child;
    }

    private Map<String, FolderResponse> resolveFolderTree(Long ownerId) {
        try {
            var response = folderServiceClient.getAllFolders(ownerId);
            if (response == null || response.getData() == null) {
                return new HashMap<>();
            }
            Map<String, FolderResponse> byId = new HashMap<>();
            for (FolderResponse folder : response.getData()) {
                byId.put(String.valueOf(folder.getId()), folder);
            }
            return byId;
        } catch (Exception e) {
            log.warn("Failed to resolve folder tree for ownerId={}: {}", ownerId, e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, List<String>> buildChildrenIndex(Map<String, FolderResponse> foldersById) {
        Map<String, List<String>> index = new HashMap<>();
        for (FolderResponse folder : foldersById.values()) {
            if (folder.getParentFolderId() != null) {
                index.computeIfAbsent(String.valueOf(folder.getParentFolderId()), k -> new ArrayList<>())
                        .add(String.valueOf(folder.getId()));
            }
        }
        return index;
    }

    private void collectDescendants(String folderId, Map<String, FolderResponse> foldersById,
                                    Map<String, List<String>> childrenByParent, Set<String> out) {
        if (!out.add(folderId)) {
            return; // already visited (cycle guard)
        }
        for (String child : childrenByParent.getOrDefault(folderId, List.of())) {
            if (foldersById.containsKey(child)) {
                collectDescendants(child, foldersById, childrenByParent, out);
            }
        }
    }

    private FileMetadata findActiveOwnedFile(Long fileId, Long ownerId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        if (!metadata.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("You do not have access to this file");
        }
        if (metadata.getStatus() != FileStatus.ACTIVE) {
            throw new BadRequestException("File has been deleted: " + fileId);
        }
        return metadata;
    }

    /** A single archive entry: relative path + source file. */
    private record Entry(String path, FileMetadata file) {
    }
}
