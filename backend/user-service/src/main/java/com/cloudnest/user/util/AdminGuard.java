package com.cloudnest.user.util;

import com.cloudnest.user.exception.AdminAccessDeniedException;

/**
 * Small guard for admin-only endpoints.
 * <p>
 * The {@code X-User-Role} header is set by the API Gateway from the validated
 * JWT (overwriting any caller-supplied value), and by the Auth Service's Feign
 * interceptor for internal provisioning calls.
 */
public final class AdminGuard {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AdminGuard() {
        // Utility class — prevent instantiation
    }

    /**
     * Rejects the call unless the caller carries the admin role.
     */
    public static void requireAdmin(String roleHeader) {
        if (!ROLE_ADMIN.equalsIgnoreCase(roleHeader)) {
            throw new AdminAccessDeniedException("Admin role required to access this endpoint");
        }
    }
}
