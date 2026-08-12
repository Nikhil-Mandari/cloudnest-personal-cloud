package com.cloudnest.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Storage analytics overview for the authenticated user.
 * <p>
 * Drives the frontend Analytics page (GET /api/files/stats/overview): total
 * usage, file/folder/trash counts, largest files, file-type breakdown and
 * weekly/monthly upload trends. All numbers are scoped to one owner.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageOverviewResponse {

    /** Total bytes of active files. */
    private long storageUsed;

    /** Number of active (non-trashed) files. */
    private long fileCount;

    /** Number of folders owned by the user (including empty ones). */
    private long folderCount;

    /** Number of soft-deleted (trashed) files. */
    private long trashFileCount;

    /** Total bytes of trashed files. */
    private long trashSize;

    /** Top files by size (largest first). */
    private List<LargestFileInfo> largestFiles;

    /** Storage grouped by file-type category. */
    private List<FileTypeStat> fileTypeStats;

    /** Bytes uploaded per day over the last 7 days. */
    private List<UsagePoint> weeklyUsage;

    /** Bytes uploaded per month over the last 6 months. */
    private List<UsagePoint> monthlyUsage;

    /** Top file by size. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LargestFileInfo {
        private Long id;
        private String originalFileName;
        private Long fileSize;
        private String fileType;
        private String folderId;
        private String uploadedAt;
    }

    /** Storage grouped by a file-type category. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileTypeStat {
        private String category;
        private long bytes;
        private long count;
    }

    /** Bytes-uploaded point on a usage timeline. */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsagePoint {
        private String label;
        private long bytes;
    }
}
