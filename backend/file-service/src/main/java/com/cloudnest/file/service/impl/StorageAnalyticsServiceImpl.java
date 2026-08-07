package com.cloudnest.file.service.impl;

import com.cloudnest.file.client.FolderServiceClient;
import com.cloudnest.file.dto.AdminStorageOverviewResponse;
import com.cloudnest.file.dto.FileTypeStat;
import com.cloudnest.file.dto.LargestFileInfo;
import com.cloudnest.file.dto.StorageOverviewResponse;
import com.cloudnest.file.dto.UsagePoint;
import com.cloudnest.file.entity.FileMetadata;
import com.cloudnest.file.entity.FileMetadata.FileStatus;
import com.cloudnest.file.repository.FileMetadataRepository;
import com.cloudnest.file.service.StorageAnalyticsService;
import com.cloudnest.file.util.FileTypeCategorizer;
import com.cloudnest.file.util.FileTypeCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link StorageAnalyticsService}.
 * <p>
 * Aggregates are computed in memory over the owner's file metadata (personal
 * cloud scale); the folder count is resolved through the Folder Service and
 * fails open to zero when that service is unreachable.
 */
@Slf4j
@Service
public class StorageAnalyticsServiceImpl implements StorageAnalyticsService {

    private static final int TOP_LARGEST_FILES = 10;
    private static final int WEEKS_BACK = 8;
    private static final int MONTHS_BACK = 12;

    private final FileMetadataRepository fileMetadataRepository;
    private final FolderServiceClient folderServiceClient;

    public StorageAnalyticsServiceImpl(
            FileMetadataRepository fileMetadataRepository,
            FolderServiceClient folderServiceClient) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.folderServiceClient = folderServiceClient;
    }

    @Override
    @Transactional(readOnly = true)
    public StorageOverviewResponse getOverview(Long ownerId) {
        List<FileMetadata> active = fileMetadataRepository.findActiveByOwnerId(ownerId);
        List<FileMetadata> trash = fileMetadataRepository.findByOwnerIdAndStatus(ownerId, FileStatus.DELETED);

        long storageUsed = active.stream().mapToLong(FileMetadata::getFileSize).sum();
        long trashSize = trash.stream().mapToLong(FileMetadata::getFileSize).sum();

        // ── Largest files ─────────────────────────────────────────────────────
        List<LargestFileInfo> largest = active.stream()
                .sorted(Comparator.comparing(FileMetadata::getFileSize).reversed())
                .limit(TOP_LARGEST_FILES)
                .map(file -> LargestFileInfo.builder()
                        .id(file.getId())
                        .originalFileName(file.getOriginalFileName())
                        .contentType(file.getContentType())
                        .fileSize(file.getFileSize())
                        .uploadedAt(file.getUploadedAt() != null ? file.getUploadedAt() : file.getCreatedAt())
                        .build())
                .toList();

        // ── File-type breakdown ────────────────────────────────────────────────
        Map<FileTypeCategory, long[]> buckets = new EnumMap<>(FileTypeCategory.class);
        for (FileMetadata file : active) {
            FileTypeCategory category = FileTypeCategorizer.categorize(
                    file.getContentType(), file.getOriginalFileName());
            long[] agg = buckets.computeIfAbsent(category, c -> new long[]{0, 0});
            agg[0]++;                    // count
            agg[1] += safeSize(file);    // bytes
        }
        List<FileTypeStat> typeStats = buckets.entrySet().stream()
                .map(entry -> FileTypeStat.builder()
                        .category(entry.getKey().name().toLowerCase())
                        .count(entry.getValue()[0])
                        .totalBytes(entry.getValue()[1])
                        .build())
                .sorted(Comparator.comparingLong(FileTypeStat::getTotalBytes).reversed())
                .toList();

        // ── Usage over time ───────────────────────────────────────────────────
        List<UsagePoint> weekly = weeklyUsage(active);
        List<UsagePoint> monthly = monthlyUsage(active);

        return StorageOverviewResponse.builder()
                .storageUsed(storageUsed)
                .fileCount(active.size())
                .folderCount(resolveFolderCount(ownerId))
                .trashFileCount(trash.size())
                .trashSize(trashSize)
                .largestFiles(largest)
                .fileTypeStats(typeStats)
                .weeklyUsage(weekly)
                .monthlyUsage(monthly)
                .build();
    }

    private List<UsagePoint> weeklyUsage(List<FileMetadata> files) {
        LocalDate today = LocalDate.now();
        List<UsagePoint> points = new ArrayList<>(WEEKS_BACK);
        for (int i = WEEKS_BACK - 1; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDateTime from = weekStart.atStartOfDay();
            LocalDateTime to = weekStart.plusWeeks(1).atStartOfDay();
            long bytes = files.stream()
                    .filter(file -> isInWindow(uploadedAt(file), from, to))
                    .mapToLong(this::safeSize)
                    .sum();
            points.add(UsagePoint.builder()
                    .label(weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .start(weekStart.atStartOfDay().toString())
                    .bytes(bytes)
                    .build());
        }
        return points;
    }

    private List<UsagePoint> monthlyUsage(List<FileMetadata> files) {
        YearMonth current = YearMonth.now();
        List<UsagePoint> points = new ArrayList<>(MONTHS_BACK);
        for (int i = MONTHS_BACK - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDateTime from = month.atDay(1).atStartOfDay();
            LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
            long bytes = files.stream()
                    .filter(file -> isInWindow(uploadedAt(file), from, to))
                    .mapToLong(this::safeSize)
                    .sum();
            points.add(UsagePoint.builder()
                    .label(month.format(DateTimeFormatter.ofPattern("MMM yy")))
                    .start(from.toString())
                    .bytes(bytes)
                    .build());
        }
        return points;
    }

    private boolean isInWindow(LocalDateTime time, LocalDateTime from, LocalDateTime to) {
        return time != null && !time.isBefore(from) && time.isBefore(to);
    }

    private LocalDateTime uploadedAt(FileMetadata file) {
        return file.getUploadedAt() != null ? file.getUploadedAt() : file.getCreatedAt();
    }

    private long safeSize(FileMetadata file) {
        return file.getFileSize() != null ? file.getFileSize() : 0L;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStorageOverviewResponse getAdminOverview() {
        List<FileMetadata> active = fileMetadataRepository.findByStatus(FileStatus.ACTIVE);
        List<FileMetadata> trash = fileMetadataRepository.findByStatus(FileStatus.DELETED);

        long totalBytes = active.stream().mapToLong(this::safeSize).sum();
        long trashSize = trash.stream().mapToLong(this::safeSize).sum();
        long distinctOwners = fileMetadataRepository.countDistinctOwners();

        // ── Largest files across the platform ──────────────────────────────────
        List<LargestFileInfo> largest = active.stream()
                .sorted(Comparator.comparing(FileMetadata::getFileSize).reversed())
                .limit(TOP_LARGEST_FILES)
                .map(file -> LargestFileInfo.builder()
                        .id(file.getId())
                        .originalFileName(file.getOriginalFileName())
                        .contentType(file.getContentType())
                        .fileSize(file.getFileSize())
                        .uploadedAt(uploadedAt(file))
                        .build())
                .toList();

        // ── File-type breakdown across all users ───────────────────────────────
        Map<FileTypeCategory, long[]> buckets = new EnumMap<>(FileTypeCategory.class);
        for (FileMetadata file : active) {
            FileTypeCategory category = FileTypeCategorizer.categorize(
                    file.getContentType(), file.getOriginalFileName());
            long[] agg = buckets.computeIfAbsent(category, c -> new long[]{0, 0});
            agg[0]++;
            agg[1] += safeSize(file);
        }
        List<FileTypeStat> typeStats = buckets.entrySet().stream()
                .map(entry -> FileTypeStat.builder()
                        .category(entry.getKey().name().toLowerCase())
                        .count(entry.getValue()[0])
                        .totalBytes(entry.getValue()[1])
                        .build())
                .sorted(Comparator.comparingLong(FileTypeStat::getTotalBytes).reversed())
                .toList();

        return AdminStorageOverviewResponse.builder()
                .totalUsers(distinctOwners)
                .totalFiles(active.size())
                .totalBytes(totalBytes)
                .trashFileCount(trash.size())
                .trashSize(trashSize)
                .largestFiles(largest)
                .fileTypeStats(typeStats)
                .weeklyUsage(weeklyUsage(active))
                .monthlyUsage(monthlyUsage(active))
                .build();
    }

    private long resolveFolderCount(Long ownerId) {
        try {
            var response = folderServiceClient.getAllFolders(ownerId);
            if (response != null && response.getData() != null) {
                return response.getData().size();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve folder count for ownerId={}: {}", ownerId, e.getMessage());
        }
        return 0;
    }
}
