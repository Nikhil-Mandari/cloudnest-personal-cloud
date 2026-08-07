package com.cloudnest.auth.security;

import java.util.Locale;

/**
 * Lightweight User-Agent parser that classifies the browser, operating
 * system and device category without any external dependency.
 * <p>
 * Deliberately conservative: anything it cannot classify falls back to
 * {@code Unknown} rather than guessing.
 */
public final class DeviceInfoParser {

    private DeviceInfoParser() {
        // Utility class — prevent instantiation
    }

    /**
     * Builds a {@link DeviceInfo} from the raw {@code User-Agent} header and
     * the client-supplied stable device id.
     *
     * @param userAgent raw {@code User-Agent} header (may be {@code null})
     * @param deviceId  stable client device identifier (may be {@code null})
     * @return a populated {@link DeviceInfo}
     */
    public static DeviceInfo parse(String userAgent, String deviceId) {
        String ua = userAgent == null ? "" : userAgent;
        String id = (deviceId == null || deviceId.isBlank()) ? "unknown-device" : deviceId;

        String browser = browserOf(ua);
        String os = osOf(ua);
        String type = deviceTypeOf(ua);
        String name = humanName(browser, os);

        return new DeviceInfo(id, name, browser, os, type);
    }

    // -- Private helpers -----------------------------------------------------

    private static String browserOf(String ua) {
        if (contains(ua, "Edg/")) return "Edge";
        if (contains(ua, "OPR/") || contains(ua, "Opera")) return "Opera";
        if (contains(ua, "Chrome/") && !contains(ua, "Chromium")) return "Chrome";
        if (contains(ua, "Firefox/")) return "Firefox";
        if (contains(ua, "Safari/")) return "Safari";
        if (contains(ua, "Trident/") || contains(ua, "MSIE")) return "Internet Explorer";
        if (contains(ua, "Postman")) return "Postman";
        if (contains(ua, "curl/")) return "cURL";
        if (contains(ua, "curl")) return "cURL";
        return DeviceInfo.UNKNOWN;
    }

    private static String osOf(String ua) {
        if (contains(ua, "Windows")) return "Windows";
        if (contains(ua, "Android")) return "Android";
        if (contains(ua, "iPhone") || contains(ua, "iPad") || contains(ua, "iOS")) return "iOS";
        if (contains(ua, "Mac OS X") || contains(ua, "Macintosh")) return "macOS";
        if (contains(ua, "Linux")) return "Linux";
        if (contains(ua, "CrOS")) return "ChromeOS";
        return DeviceInfo.UNKNOWN;
    }

    private static String deviceTypeOf(String ua) {
        if (contains(ua, "iPad")) {
            return "TABLET";
        }
        // Android tablets often omit the "Tablet" token but never say "Mobile".
        if (contains(ua, "Android") && !contains(ua, "Mobile")) {
            return "TABLET";
        }
        if (contains(ua, "Tablet")) {
            return "TABLET";
        }
        if (contains(ua, "Mobile") || contains(ua, "iPhone")) {
            return "MOBILE";
        }
        if (contains(ua, "Windows") || contains(ua, "Mac") || contains(ua, "Linux") || contains(ua, "X11")) {
            return "DESKTOP";
        }
        return "OTHER";
    }

    private static String humanName(String browser, String os) {
        if (DeviceInfo.UNKNOWN.equals(browser) && DeviceInfo.UNKNOWN.equals(os)) {
            return "Unknown device";
        }
        if (DeviceInfo.UNKNOWN.equals(os)) {
            return browser;
        }
        return browser + " on " + os;
    }

    private static boolean contains(String value, String token) {
        return value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }
}
