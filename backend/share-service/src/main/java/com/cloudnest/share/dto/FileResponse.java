package com.cloudnest.share.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing a file, received from the File Service via Feign.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileResponse {

    private Long id;
    private String fileId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private Long ownerId;
    private Long folderId;
    private String status;
}
