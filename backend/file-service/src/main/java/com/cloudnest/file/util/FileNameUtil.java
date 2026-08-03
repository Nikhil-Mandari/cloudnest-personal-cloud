package com.cloudnest.file.util;

import java.util.UUID;

/**
 * Utility for sanitising client-supplied file names and generating unique
 * MinIO object names.
 * <p>
 * Object names follow the {@code UUID_originalFilename} pattern required by
 * CloudNest, which guarantees uniqueness while keeping the original name
 * human-readable in the object store.
 */
public final class FileNameUtil {

    /** Fallback name used when a client sends an empty / unusable file name. */
    public static final String DEFAULT_FILE_NAME = "file";

    private FileNameUtil() {
        // Utility class — not instantiable
    }

    /**
     * Sanitises a client-supplied file name.
     * <p>
     * Strips any path components (e.g. browsers may send
     * {@code C:\fakepath\report.pdf} or {@code /tmp/report.pdf}), trims
     * whitespace, and removes control characters. Falls back to
     * {@value #DEFAULT_FILE_NAME} if nothing usable remains.
     *
     * @param originalFileName the raw client-supplied file name
     * @return a safe base file name without any path components
     */
    public static String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return DEFAULT_FILE_NAME;
        }

        // Normalise Windows separators and keep only the last path segment
        String normalized = originalFileName.replace('\\', '/');
        String baseName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();

        if (baseName.isBlank() || baseName.equals(".") || baseName.equals("..")) {
            return DEFAULT_FILE_NAME;
        }

        // Remove control characters (including newlines) that could corrupt headers
        return baseName.replaceAll("[\\x00-\\x1F\\x7F]", "");
    }

    /**
     * Generates a unique MinIO object name using the pattern
     * {@code UUID_originalFilename}.
     *
     * @param originalFileName the original (sanitised) file name
     * @return a unique object name, e.g. {@code 8f2a..._report.pdf}
     */
    public static String generateObjectName(String originalFileName) {
        return UUID.randomUUID() + "_" + sanitizeFileName(originalFileName);
    }
}
