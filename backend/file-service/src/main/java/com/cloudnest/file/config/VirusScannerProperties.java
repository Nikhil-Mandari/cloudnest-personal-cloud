package com.cloudnest.file.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the virus scanner.
 * <p>
 * Bound from the {@code virus-scanner.*} prefix in config-repo:
 * <ul>
 *   <li>{@code provider=noop} (default) — a pass-through scanner that marks
 *       every upload CLEAN (used in dev / when no antivirus is available)</li>
 *   <li>{@code provider=clamav} — scans through a ClamAV daemon (clamd) using
 *       the INSTREAM protocol over TCP</li>
 * </ul>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "virus-scanner")
public class VirusScannerProperties {

    /**
     * Scanner backend: {@code noop} (default) or {@code clamav}.
     */
    private String provider = "noop";

    /**
     * ClamAV daemon host (only used when provider=clamav).
     */
    private String clamdHost = "localhost";

    /**
     * ClamAV daemon port (only used when provider=clamav).
     */
    private int clamdPort = 3310;

    /**
     * Socket timeout in milliseconds for the ClamAV connection.
     */
    private int timeoutMs = 30_000;
}
