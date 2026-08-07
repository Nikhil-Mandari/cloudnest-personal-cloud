package com.cloudnest.file.util;

import java.util.Locale;
import java.util.Set;

/**
 * Maps a MIME content type (and, as a fallback for generic types like
 * {@code application/octet-stream}, the file extension) to a coarse
 * {@link FileTypeCategory} for analytics aggregation.
 */
public final class FileTypeCategorizer {

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "js", "mjs", "cjs", "ts", "tsx", "jsx", "py", "java", "go", "rs", "c", "h", "cpp",
            "hpp", "cs", "rb", "php", "swift", "kt", "scala", "sh", "bash", "zsh", "ps1",
            "html", "htm", "css", "scss", "sass", "less", "json", "xml", "yaml", "yml", "toml",
            "ini", "conf", "sql", "vue", "svelte", "graphql", "proto", "dockerfile", "gradle"
    );

    private FileTypeCategorizer() {
        // Utility class — not instantiable
    }

    /**
     * Categorises a file by its MIME content type, falling back to the file
     * extension for generic types.
     *
     * @param contentType    the MIME content type (may be null)
     * @param originalFileName the original file name (may be null)
     * @return the coarse category
     */
    public static FileTypeCategory categorize(String contentType, String originalFileName) {
        String mime = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (mime.startsWith("image/")) {
            return FileTypeCategory.IMAGE;
        }
        if (mime.startsWith("video/")) {
            return FileTypeCategory.VIDEO;
        }
        if (mime.startsWith("audio/")) {
            return FileTypeCategory.AUDIO;
        }
        if (mime.equals("application/pdf")) {
            return FileTypeCategory.PDF;
        }
        if (isArchive(mime)) {
            return FileTypeCategory.ARCHIVE;
        }
        if (isOffice(mime)) {
            return FileTypeCategory.DOCUMENT;
        }
        if (mime.startsWith("text/") || mime.startsWith("application/")) {
            // text/* and most application/* are documents unless the extension
            // points at source code
            return categorizeByExtension(originalFileName);
        }
        return FileTypeCategory.OTHER;
    }

    private static boolean isArchive(String mime) {
        return mime.contains("zip") || mime.contains("gzip") || mime.contains("tar")
                || mime.contains("compressed") || mime.contains("rar") || mime.contains("7z");
    }

    private static boolean isOffice(String mime) {
        return mime.contains("msword")
                || mime.contains("openxmlformats")
                || mime.contains("ms-excel")
                || mime.contains("ms-powerpoint")
                || mime.contains("rtf")
                || mime.contains("opendocument");
    }

    private static FileTypeCategory categorizeByExtension(String originalFileName) {
        if (originalFileName == null) {
            return FileTypeCategory.DOCUMENT;
        }
        String name = originalFileName.toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return FileTypeCategory.DOCUMENT;
        }
        String ext = name.substring(dot + 1);

        if (CODE_EXTENSIONS.contains(ext)) {
            return FileTypeCategory.CODE;
        }
        if (ext.equals("txt") || ext.equals("md") || ext.equals("rst") || ext.equals("log")) {
            return FileTypeCategory.DOCUMENT;
        }
        return FileTypeCategory.DOCUMENT;
    }
}
