package com.cloudnest.auth.util;

import com.cloudnest.auth.dto.DeviceInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a {@link DeviceInfo} from the incoming request headers:
 * {@code X-Device-Id} (client-stable device id), {@code User-Agent} (browser,
 * OS, form factor) and the client IP.
 */
@Component
public class DeviceInfoParser {

    private static final Pattern BROWSER_PATTERN = Pattern.compile(
            "(Edge|EdgA?/|OPR/|Chrome|Firefox|Safari|Version/\\d[\\d.]* Mobile Safari)", Pattern.CASE_INSENSITIVE);

    private static final Pattern OS_PATTERN = Pattern.compile(
            "(Windows NT [\\d.]+|Mac OS X [\\d_]+|Android [\\d.]+|iPhone OS [\\d_]+|iPadOS [\\d_]+|Linux|CrOS [\\d.]+)",
            Pattern.CASE_INSENSITIVE);

    public DeviceInfo parse(HttpServletRequest request) {
        String deviceId = firstNonBlank(
                request.getHeader("X-Device-Id"),
                request.getHeader("x-device-id"));
        String userAgent = request.getHeader("User-Agent");
        String ip = clientIp(request);

        String browser = detectBrowser(userAgent);
        String os = detectOs(userAgent);
        String deviceType = detectType(userAgent, deviceId);
        String deviceName = deviceId != null && deviceId.length() >= 6
                ? "Device " + deviceId.substring(0, Math.min(8, deviceId.length())).toUpperCase(Locale.ROOT)
                : "Browser session";

        return DeviceInfo.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .browser(browser)
                .os(os)
                .deviceType(deviceType)
                .ipAddress(ip)
                .location("Unknown")
                .build();
    }

    /** Parses device info without a servlet request (used by internal callers). */
    public DeviceInfo from(String deviceId, String userAgent, String ip) {
        return DeviceInfo.builder()
                .deviceId(deviceId)
                .deviceName(deviceId != null && deviceId.length() >= 6
                        ? "Device " + deviceId.substring(0, Math.min(8, deviceId.length())).toUpperCase(Locale.ROOT)
                        : "Browser session")
                .browser(detectBrowser(userAgent))
                .os(detectOs(userAgent))
                .deviceType(detectType(userAgent, deviceId))
                .ipAddress(ip)
                .location("Unknown")
                .build();
    }

    private String detectBrowser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        Matcher m = BROWSER_PATTERN.matcher(userAgent);
        if (m.find()) {
            String token = m.group().toLowerCase(Locale.ROOT);
            if (token.startsWith("edg") || token.startsWith("edge")) {
                return "Edge";
            }
            if (token.startsWith("opr")) {
                return "Opera";
            }
            if (token.contains("chrome")) {
                return "Chrome";
            }
            if (token.contains("firefox")) {
                return "Firefox";
            }
            if (token.contains("safari")) {
                return "Safari";
            }
        }
        return "Unknown";
    }

    private String detectOs(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        Matcher m = OS_PATTERN.matcher(userAgent);
        if (m.find()) {
            return m.group().replace("_", ".");
        }
        return "Unknown";
    }

    private String detectType(String userAgent, String deviceId) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")
                || (ua.contains("mobile") && ua.contains("safari"))) {
            return "MOBILE";
        }
        if (ua.contains("ipad") || ua.contains("tablet")
                || (ua.contains("android") && !ua.contains("mobile"))) {
            return "TABLET";
        }
        if (ua.contains("windows") || ua.contains("mac os") || ua.contains("linux") || ua.contains("x11")) {
            return "DESKTOP";
        }
        return deviceId != null ? "OTHER" : "DESKTOP";
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
