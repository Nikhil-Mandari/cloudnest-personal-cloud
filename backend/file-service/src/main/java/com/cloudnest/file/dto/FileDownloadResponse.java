package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * Internal DTO carrying streamed file content for download / preview endpoints.
 * <p>
 * Not serialised to JSON — the controller consumes it and streams the content
 * back to the client as a binary resource.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Internal container for streamed file content (download / preview)")
public class FileDownloadResponse {

    @Schema(description = "Original file name", example = "report.pdf")
    private String originalFileName;

    @Schema(description = "MIME content type", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size in bytes", example = "204800")
    private long fileSize;

    @Schema(description = "Streamed binary content (not serialised)")
    private InputStream inputStream;
}
