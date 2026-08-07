package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single time-bucket of storage usage (weekly or monthly).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage usage for one time bucket")
public class UsagePoint {

    @Schema(description = "Bucket label (ISO week start date or 'MMM yy')", example = "2026-07-06")
    private String label;

    @Schema(description = "ISO timestamp of the bucket start", example = "2026-07-06T00:00:00")
    private String start;

    @Schema(description = "Bytes stored in the bucket", example = "52428800")
    private long bytes;
}
