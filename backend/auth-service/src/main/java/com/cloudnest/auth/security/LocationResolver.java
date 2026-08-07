package com.cloudnest.auth.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves an approximate human-readable location for an IP address.
 * <p>
 * The default implementation keeps the platform self-contained and
 * GDPR-friendly: no outbound geolocation calls are made. Production
 * deployments can swap in a GeoIP provider (e.g. MaxMind GeoLite2) by
 * implementing this interface and overriding the bean.
 */
public interface LocationResolver {

    /**
     * @param ipAddress the client IP (may be {@code null})
     * @return an approximate location, e.g. {@code "Bengaluru, India"}
     */
    String resolve(String ipAddress);

    /**
     * Default no-op implementation.
     */
    @Slf4j
    @Component
    class DefaultLocationResolver implements LocationResolver {

        @Override
        public String resolve(String ipAddress) {
            if (ipAddress == null || ipAddress.isBlank()) {
                return "Unknown location";
            }
            // Private / loopback ranges are local — nothing useful to derive.
            if (isPrivate(ipAddress)) {
                return "Local network";
            }
            log.debug("No GeoIP provider configured — location for IP {} left unknown", ipAddress);
            return "Unknown location";
        }

        private boolean isPrivate(String ip) {
            if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("127.")
                    || ip.startsWith("169.254.") || ip.startsWith("::1")) {
                return true;
            }
            // 172.16.0.0 – 172.31.255.255
            if (ip.startsWith("172.")) {
                try {
                    int second = Integer.parseInt(ip.split("\\.")[1]);
                    return second >= 16 && second <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
    }
}
