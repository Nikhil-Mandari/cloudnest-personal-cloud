package com.cloudnest.file.dto;

import com.cloudnest.file.entity.DuplicateAction;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of an upload attempt, including duplicate detection.
 * <p>
 * When {@code duplicate} is {@code true} the upload either did not create
 * anything ({@code SKIP} / {@code ASK}) or replaced the existing file's
 * content ({@code REPLACE}); {@code duplicateOf} describes the existing file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Upload result with duplicate detection metadata")
public class UploadResultResponse {

    @Schema(description = "Whether an identical-content file already exists", example = "false")
    private Boolean duplicate;

    @Schema(description = "Duplicate action that was applied", example = "ASK")
    private DuplicateAction actionTaken;

    @Schema(description = "Created or updated file (null when the upload was skipped)")
    private FileResponse file;

    @Schema(description = "Existing file with identical content (null when no duplicate)")
    private DuplicateFileInfo duplicateOf;
}
