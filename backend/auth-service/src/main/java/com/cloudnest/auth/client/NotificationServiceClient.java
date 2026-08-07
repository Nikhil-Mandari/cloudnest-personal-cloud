package com.cloudnest.auth.client;

import com.cloudnest.auth.config.NotificationServiceClientConfig;
import com.cloudnest.auth.dto.NotificationCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the Notification Service's internal creation endpoint.
 * <p>
 * In-app security notifications (new sign-ins, unknown devices, password
 * changes) are fire-and-forget: the auth flow must never depend on their
 * delivery, so callers treat failures as non-fatal. The configured request
 * interceptor presents the service identity headers.
 */
@FeignClient(name = "notification-service", path = "/api/notifications",
        configuration = NotificationServiceClientConfig.class)
public interface NotificationServiceClient {

    /**
     * Notification type names, mirroring the notification-service
     * {@code NotificationType} enum values.
     */
    String TYPE_LOGIN_ALERT = "LOGIN_ALERT";
    String TYPE_UNKNOWN_DEVICE_LOGIN = "UNKNOWN_DEVICE_LOGIN";
    String TYPE_PASSWORD_CHANGED = "PASSWORD_CHANGED";
    String TYPE_PASSWORD_RESET = "PASSWORD_RESET";
    String TYPE_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    String TYPE_TWO_FACTOR_ENABLED = "TWO_FACTOR_ENABLED";
    String TYPE_TWO_FACTOR_DISABLED = "TWO_FACTOR_DISABLED";
    String TYPE_BACKUP_CODES_REGENERATED = "BACKUP_CODES_REGENERATED";
    String TYPE_PASSKEY_REGISTERED = "PASSKEY_REGISTERED";
    String TYPE_PASSKEY_REMOVED = "PASSKEY_REMOVED";

    /**
     * Creates an in-app notification for a user.
     *
     * @param request the notification creation payload
     */
    @PostMapping
    void create(@RequestBody NotificationCreateRequest request);
}
