package com.cloudnest.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Builds a {@link ClientInfo} from an incoming HTTP request using the same
 * device-parsing / IP-resolution / location-resolution chain as the
 * {@code AuthController} private helper — extracted so every controller
 * (auth, 2FA, passkeys) shares one implementation.
 */
@Component
public class ClientInfoFactory {

    /** Header carrying the stable client device id (set by the frontend). */
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final LocationResolver locationResolver;

    public ClientInfoFactory(LocationResolver locationResolver) {
        this.locationResolver = locationResolver;
    }

    /**
     * Resolves the client context for the current request.
     */
    public ClientInfo from(HttpServletRequest request) {
        DeviceInfo device = DeviceInfoParser.parse(
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(DEVICE_ID_HEADER));
        String ip = IpUtils.resolve(request);
        return new ClientInfo(ip, device, locationResolver.resolve(ip));
    }
}
