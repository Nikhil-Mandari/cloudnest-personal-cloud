package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Device / request metadata captured from headers ({@code X-Device-Id},
 * {@code User-Agent}, client IP) and attached to sessions, trusted devices and
 * audit records.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceInfo {

    /** Stable client device id (the frontend sends {@code X-Device-Id}). */
    private String deviceId;

    /** Human-readable device label derived from the user agent. */
    private String deviceName;

    @Builder.Default
    private String browser = "Unknown";

    @Builder.Default
    private String os = "Unknown";

    /** DESKTOP / TABLET / MOBILE / OTHER. */
    @Builder.Default
    private String deviceType = "OTHER";

    private String ipAddress;

    @Builder.Default
    private String location = "Unknown";
}
