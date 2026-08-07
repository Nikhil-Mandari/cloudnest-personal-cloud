package com.cloudnest.share.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.InputStream;

/**
 * Internal container carrying the streamed content of a shared file download.
 * <p>
 * Not serialised to JSON — the controller consumes it and streams the binary
 * content back to the client.
 */
@Getter
@Builder
@AllArgsConstructor
public class ShareDownloadResponse {

    /** Streamed binary content (caller must close it). */
    private final InputStream inputStream;

    /** Original file name for the Content-Disposition header. */
    private final String originalFileName;

    /** MIME content type. */
    private final String contentType;

    /** Content length in bytes (-1 when unknown). */
    private final long fileSize;
}
