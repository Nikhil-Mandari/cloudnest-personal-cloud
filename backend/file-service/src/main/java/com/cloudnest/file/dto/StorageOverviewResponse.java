package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full storage analytics overview for a user: totals, largest files, file-type
 * breakdown and usage over time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Storage analytics overview for the authenticated user")
public class StorageOverviewResponse {

    @Schema(description = "Total bytes of active files", example = "1048576000")
    private long storageUsed;

    @Schema(description = "Number of active files", example = "120")
    private long fileCount;

    @Schema(description = "Number of folders (resolved from the Folder Service)", example = "14")
    private long folderCount;

    @Schema(description = "Number of files in the trash", example = "3")
    private long trashFileCount;

    @Schema(description = "Total bytes in the trash", example = "10485760")
    private long trashSize;

    @Schema(description = "Top files by size (max 10)")
    private List<LargestFileInfo> largestFiles;

    @Schema(description = "Storage grouped by file-type category")
    private List<FileTypeStat> fileTypeStats;

    @Schema(description = "Bytes uploaded per week, last 8 weeks")
    private List<UsagePoint> weeklyUsage;

    @Schema(description = "Bytes uploaded per month, last 12 months")
    private List<UsagePoint> monthlyUsage;
}
