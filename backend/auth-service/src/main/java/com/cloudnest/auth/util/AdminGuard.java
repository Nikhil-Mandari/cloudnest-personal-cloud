package com.cloudnest.auth.util;

import com.cloudnest.auth.exception.AdminAccessDeniedException;

/**
 * Small guard for admin-only endpoints.
 * <p>
 * The {@code X-User-Role} header is set by the API Gateway from the validated
 * JWT (and any caller-supplied value is overwritten there), so it is trusted
 * at the service boundary.
 */
public final class AdminGuard {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AdminGuard() {
        // Utility class — prevent instantiation
    }

    /**
     * Rejects the call unless the caller carries the admin role.
     *
     * @param roleHeader the {@code X-User-Role} header value (may be null)
     * @throws AdminAccessDeniedException when the caller is not an admin
     */
    public static void requireAdmin(String roleHeader) {
        if (!ROLE_ADMIN.equalsIgnoreCase(roleHeader)) {
            throw new AdminAccessDeniedException("Admin role required to access this endpoint");
        }
    }

    /**
     * Whether the given role value denotes an administrator.
     */
    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role);
    }
}
