package com.cloudnest.auth.security;

/**
 * Everything the Auth Service knows about the caller of a request:
 * resolved IP address, parsed device info and approximate location.
 *
 * @param ipAddress  client IP (may be a proxy value / null)
 * @param device     parsed device information
 * @param location   approximate location string (e.g. "City, Country")
 */
public record ClientInfo(String ipAddress, DeviceInfo device, String location) {

    public static ClientInfo unknown() {
        return new ClientInfo(null,
                new DeviceInfo(null, DeviceInfo.UNKNOWN, DeviceInfo.UNKNOWN, DeviceInfo.UNKNOWN, "OTHER"),
                "Unknown location");
    }
}
