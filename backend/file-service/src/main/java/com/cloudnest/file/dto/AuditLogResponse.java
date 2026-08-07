package com.cloudnest.file.dto;

import com.cloudnest.file.entity.AuditLog.AuditAction;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for a single audit-trail entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Single audit-trail entry")
public class AuditLogResponse {

    @Schema(description = "Audit log primary key", example = "42")
    private Long id;

    @Schema(description = "Owner whose action produced the entry (admin views)", example = "7")
    private Long ownerId;

    @Schema(description = "Audited action", example = "UPLOAD")
    private AuditAction action;

    @Schema(description = "Resource kind (FILE / VERSION / …)", example = "FILE")
    private String resourceType;

    @Schema(description = "Resource identifier", example = "12")
    private String resourceId;

    @Schema(description = "Resource display name", example = "report.pdf")
    private String resourceName;

    @Schema(description = "Free-form detail", example = "2.0 MB")
    private String details;

    @Schema(description = "Originating IP address (best effort)", example = "203.0.113.7")
    private String ipAddress;

    @Schema(description = "Originating user agent (best effort)", example = "Mozilla/5.0 …")
    private String userAgent;

    @Schema(description = "Timestamp of the action", example = "2026-08-04T09:12:00")
    private LocalDateTime createdAt;
}
