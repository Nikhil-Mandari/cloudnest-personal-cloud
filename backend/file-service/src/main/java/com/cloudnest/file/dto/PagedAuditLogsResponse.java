package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paged view of the audit trail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paged audit-trail response")
public class PagedAuditLogsResponse {

    @Schema(description = "Entries on this page")
    private List<AuditLogResponse> content;

    @Schema(description = "Current page (zero-based)", example = "0")
    private int page;

    @Schema(description = "Page size", example = "20")
    private int size;

    @Schema(description = "Total entries across all pages", example = "137")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "7")
    private int totalPages;
}
