package com.cloudnest.file.service;

import com.cloudnest.file.dto.DownloadZipRequest;

import java.io.OutputStream;

/**
 * Service that streams a bulk ZIP download preserving the folder hierarchy.
 */
public interface ZipDownloadService {

    /**
     * Streams a ZIP archive of the requested files and folders into the given
     * output stream. Folder hierarchy is preserved under a {@code CloudNest/}
     * root directory.
     *
     * @param request the requested files / folders
     * @param out     the destination stream (streamed to the client)
     * @param ownerId the authenticated user's ID
     */
    void streamZip(DownloadZipRequest request, OutputStream out, Long ownerId);
}
