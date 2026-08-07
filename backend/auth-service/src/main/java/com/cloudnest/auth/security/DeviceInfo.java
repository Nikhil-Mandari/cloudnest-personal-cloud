package com.cloudnest.auth.security;

/**
 * Immutable description of the client device, derived from the
 * {@code User-Agent} header plus the stable {@code X-Device-Id} header the
 * frontend persists.
 *
 * @param deviceId   stable client-generated identifier (UUID)
 * @param deviceName human-readable device name (e.g. "Chrome on Windows")
 * @param browser    browser family (e.g. "Chrome", "Firefox", "Safari")
 * @param os         operating system (e.g. "Windows", "macOS", "Android")
 * @param deviceType device category (DESKTOP / TABLET / MOBILE / OTHER)
 */
public record DeviceInfo(
        String deviceId,
        String deviceName,
        String browser,
        String os,
        String deviceType) {

    public static final String UNKNOWN = "Unknown";
}
