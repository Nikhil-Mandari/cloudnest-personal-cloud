package com.cloudnest.file.service;

import com.cloudnest.file.entity.ScanStatus;
import com.cloudnest.file.exception.VirusDetectedException;
import com.cloudnest.file.security.ScanOutcome;
import com.cloudnest.file.security.VirusScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Facade over the configured {@link VirusScanner} backend.
 * <p>
 * Converts a raw {@link ScanOutcome} into the persisted {@link ScanStatus} and
 * raises {@link VirusDetectedException} for infected content so the caller can
 * abort the upload and remove the offending object.
 */
@Slf4j
@Service
public class VirusScanService {

    private final VirusScanner scanner;

    public VirusScanService(VirusScanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Scans the content and returns the resulting status.
     *
     * @param content    the content to scan (fully consumed)
     * @param objectName the MinIO object key, for log correlation
     * @return {@code CLEAN}, {@code INFECTED} or {@code ERROR}
     * @throws VirusDetectedException when the content is infected
     */
    public ScanStatus scan(InputStream content, String objectName) {
        ScanOutcome outcome = scanner.scan(content, objectName);
        switch (outcome) {
            case INFECTED -> {
                log.warn("Infected content blocked for object '{}'", objectName);
                throw new VirusDetectedException(
                        "A virus was detected in the uploaded content — the file was blocked");
            }
            case ERROR -> {
                log.error("Virus scan failed for object '{}' using backend '{}' — marking as ERROR",
                        objectName, scanner.name());
                return ScanStatus.ERROR;
            }
            default -> {
                log.debug("Scan clean for object '{}' using backend '{}'", objectName, scanner.name());
                return ScanStatus.CLEAN;
            }
        }
    }
}
