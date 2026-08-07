package com.cloudnest.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregate storage statistic for one file-type category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Storage aggregate for one file-type category")
public class FileTypeStat {

    @Schema(description = "File category (image / video / audio / document / pdf / archive / code / other)", example = "image")
    private String category;

    @Schema(description = "Number of files in this category", example = "24")
    private long count;

    @Schema(description = "Total bytes consumed by the category", example = "104857600")
    private long totalBytes;
}
