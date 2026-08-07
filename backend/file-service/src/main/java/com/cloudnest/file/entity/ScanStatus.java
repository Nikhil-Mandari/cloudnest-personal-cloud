package com.cloudnest.file.entity;

/**
 * Lifecycle status of the virus scan for a file's content.
 *
 * <ul>
 *   <li>{@code PENDING} — queued, not yet scanned</li>
 *   <li>{@code SCANNING} — scan in progress</li>
 *   <li>{@code CLEAN} — scanned, no threats found</li>
 *   <li>{@code INFECTED} — a threat was detected; the file is blocked</li>
 *   <li>{@code ERROR} — the scanner could not run (e.g. ClamAV unreachable)</li>
 * </ul>
 */
public enum ScanStatus {
    PENDING,
    SCANNING,
    CLEAN,
    INFECTED,
    ERROR
}
