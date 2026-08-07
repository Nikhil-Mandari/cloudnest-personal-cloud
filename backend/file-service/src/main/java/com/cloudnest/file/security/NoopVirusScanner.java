package com.cloudnest.file.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Pass-through scanner used when no antivirus backend is configured
 * ({@code virus-scanner.provider=noop}, the default).
 * <p>
 * Every upload is marked {@link ScanOutcome#CLEAN}. Content is fully drained
 * so uploads keep consuming the stream exactly once.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "virus-scanner.provider", havingValue = "noop", matchIfMissing = true)
public class NoopVirusScanner implements VirusScanner {

    @Override
    public ScanOutcome scan(InputStream content, String objectName) {
        try (content) {
            byte[] buffer = new byte[8192];
            while (content.read(buffer) != -1) {
                // Drain — nothing to analyse in noop mode.
            }
        } catch (Exception e) {
            log.warn("Noop scanner could not drain '{}': {}", objectName, e.getMessage());
            return ScanOutcome.ERROR;
        }
        return ScanOutcome.CLEAN;
    }

    @Override
    public String name() {
        return "noop";
    }
}
