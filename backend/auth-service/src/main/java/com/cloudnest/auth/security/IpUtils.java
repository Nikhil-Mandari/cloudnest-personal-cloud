package com.cloudnest.auth.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Resolves the real client IP address, honouring standard reverse-proxy
 * headers (the API Gateway sits in front of this service).
 */
public final class IpUtils {

    /** Headers inspected in priority order for the original client IP. */
    private static final List<String> FORWARDED_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "True-Client-IP");

    private IpUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Returns the client IP, falling back to the remote address.
     *
     * @param request the HTTP request
     * @return the client IP, or {@code null} when the request is {@code null}
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        for (String header : FORWARDED_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                // X-Forwarded-For is "client, proxy1, proxy2" — take the first.
                String first = value.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
