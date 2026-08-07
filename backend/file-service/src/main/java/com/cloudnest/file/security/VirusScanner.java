package com.cloudnest.file.security;

import java.io.InputStream;

/**
 * SPI for virus-scanning uploaded file content.
 * <p>
 * Implementations are selected via the {@code virus-scanner.provider}
 * property: {@code NoopVirusScanner} (default) or {@code ClamAvVirusScanner}.
 * Implementations must fully consume the input stream and must never throw —
 * connectivity problems are reported as {@link ScanOutcome#ERROR} so the
 * upload can fail open with a visible status.
 */
public interface VirusScanner {

    /**
     * Scans the given content stream.
     *
     * @param content    the content to scan (fully consumed)
     * @param objectName the MinIO object key, for log correlation
     * @return the scan outcome
     */
    ScanOutcome scan(InputStream content, String objectName);

    /**
     * Human-readable name of the backend, used in logs and status responses.
     */
    String name();
}
