package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Platform-wide storage aggregates for the admin dashboard: totals across all
 * users, largest files, file-type breakdown and usage over time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Platform-wide storage aggregates for the admin dashboard")
public class AdminStorageOverviewResponse {

    @Schema(description = "Distinct owners with at least one file record", example = "42")
    private long totalUsers;

    @Schema(description = "Active files across all users", example = "1500")
    private long totalFiles;

    @Schema(description = "Active bytes across all users", example = "5242880000")
    private long totalBytes;

    @Schema(description = "Files in the trash across all users", example = "37")
    private long trashFileCount;

    @Schema(description = "Trash bytes across all users", example = "104857600")
    private long trashSize;

    @Schema(description = "Top files by size across the platform (max 10)")
    private List<LargestFileInfo> largestFiles;

    @Schema(description = "Storage grouped by file-type category across all users")
    private List<FileTypeStat> fileTypeStats;

    @Schema(description = "Bytes uploaded per week, last 8 weeks")
    private List<UsagePoint> weeklyUsage;

    @Schema(description = "Bytes uploaded per month, last 12 months")
    private List<UsagePoint> monthlyUsage;
}
