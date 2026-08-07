package com.cloudnest.file.controller;

import com.cloudnest.file.dto.AdminStorageOverviewResponse;
import com.cloudnest.file.dto.MinioStatusResponse;
import com.cloudnest.file.dto.PagedAuditLogsResponse;
import com.cloudnest.file.service.AuditLogService;
import com.cloudnest.file.service.MinioService;
import com.cloudnest.file.service.StorageAnalyticsService;
import com.cloudnest.file.util.AdminGuard;
import com.cloudnest.file.util.StandardResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only dashboard endpoints: platform-wide storage aggregates, the
 * cross-user audit trail and the MinIO object-store status.
 * <p>
 * Every endpoint requires the {@code ROLE_ADMIN} role. The {@code X-User-Role}
 * header is set by the API Gateway from the validated JWT (overwriting any
 * caller-supplied value), so the check is trustworthy.
 */
@Slf4j
@RestController
@RequestMapping("/api/files/admin")
@Tag(name = "Admin — File Service",
        description = "Platform-wide storage, audit trail and MinIO status (admin only).")
public class AdminController {

    private final StorageAnalyticsService storageAnalyticsService;
    private final AuditLogService auditLogService;
    private final MinioService minioService;

    public AdminController(StorageAnalyticsService storageAnalyticsService,
                           AuditLogService auditLogService,
                           MinioService minioService) {
        this.storageAnalyticsService = storageAnalyticsService;
        this.auditLogService = auditLogService;
        this.minioService = minioService;
    }

    @Operation(summary = "Platform storage overview",
            description = "Returns platform-wide storage aggregates across all users: totals, " +
                    "trash usage, largest files, file-type breakdown and weekly / monthly usage. Admin only.")
    @GetMapping("/storage-overview")
    public ResponseEntity<StandardResponse<AdminStorageOverviewResponse>> storageOverview(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/files/admin/storage-overview");

        AdminStorageOverviewResponse overview = storageAnalyticsService.getAdminOverview();

        return ResponseEntity.ok(
                StandardResponse.<AdminStorageOverviewResponse>builder()
                        .success(true)
                        .message("Admin storage overview retrieved successfully")
                        .data(overview)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "Cross-user audit trail",
            description = "Returns a paged list of audit entries across every user, newest first. " +
                    "Admin only. Can be narrowed by action and / or owner.")
    @GetMapping("/audit-logs")
    public ResponseEntity<StandardResponse<PagedAuditLogsResponse>> auditLogs(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Optional action filter (e.g. UPLOAD)", example = "UPLOAD")
            @RequestParam(required = false) String action,
            @Parameter(description = "Optional owner filter", example = "7")
            @RequestParam(required = false) Long userId,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/files/admin/audit-logs - page={}, size={}, action={}, userId={}",
                page, size, action, userId);

        PagedAuditLogsResponse logs = auditLogService.getAllLogs(page, size, action, userId);

        return ResponseEntity.ok(
                StandardResponse.<PagedAuditLogsResponse>builder()
                        .success(true)
                        .message("Audit entries retrieved successfully")
                        .data(logs)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    @Operation(summary = "MinIO status",
            description = "Probes the MinIO object store and reports connectivity and bucket " +
                    "readiness. Admin only.")
    @GetMapping("/minio-status")
    public ResponseEntity<StandardResponse<MinioStatusResponse>> minioStatus(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            HttpServletRequest httpRequest) {

        AdminGuard.requireAdmin(roleHeader);
        log.info("GET /api/files/admin/minio-status");

        MinioStatusResponse status = minioService.status();

        return ResponseEntity.ok(
                StandardResponse.<MinioStatusResponse>builder()
                        .success(true)
                        .message("MinIO status retrieved successfully")
                        .data(status)
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
