package com.cloudnest.share.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Content payload streamed back through a public share link.
 * <p>
 * The Share Service resolves the share (token, expiry, password, permission)
 * and then fetches the raw bytes from the File Service via Feign. This DTO
 * carries both the bytes and the metadata needed to build the HTTP response
 * (file name, content type, size).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFileContent {

    /** Original file name, used for Content-Disposition. */
    private String originalFileName;

    /** MIME type of the file content. */
    private String contentType;

    /** Content length in bytes. */
    private Long fileSize;

    /** Raw file bytes fetched from the File Service. */
    private byte[] content;
}
