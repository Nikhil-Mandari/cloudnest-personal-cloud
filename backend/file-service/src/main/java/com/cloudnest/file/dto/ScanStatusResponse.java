package com.cloudnest.file.dto;

import com.cloudnest.file.entity.ScanStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the virus-scan status of a single file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Virus scan status of a file")
public class ScanStatusResponse {

    @Schema(description = "Public-facing file identifier (UUID)", example = "7c9e6679-…")
    private String fileId;

    @Schema(description = "Scan lifecycle status", example = "CLEAN")
    private ScanStatus scanStatus;
}
