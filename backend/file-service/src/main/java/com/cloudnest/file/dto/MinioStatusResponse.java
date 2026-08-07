package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Connectivity / readiness status of the MinIO object store, surfaced on the
 * admin dashboard. The endpoint and bucket names are non-secret configuration
 * values; credentials are never included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MinIO object-store status for the admin dashboard")
public class MinioStatusResponse {

    @Schema(description = "Configured MinIO endpoint", example = "http://minio:9000")
    private String endpoint;

    @Schema(description = "Configured bucket name", example = "cloudnest")
    private String bucket;

    @Schema(description = "Whether MinIO responded to the probe", example = "true")
    private boolean reachable;

    @Schema(description = "Whether the configured bucket exists", example = "true")
    private boolean bucketExists;

    @Schema(description = "Human-readable status summary", example = "Connected")
    private String status;
}
