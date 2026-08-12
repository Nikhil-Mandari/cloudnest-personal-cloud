package com.cloudnest.share.client;

import com.cloudnest.share.dto.NotificationCreateRequest;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign client for the Notification Service.
 * <p>
 * Used after a share is created so the recipient receives an in-app
 * notification ("Nikhil shared IMG_6660.HEIC with you"). The create endpoint
 * reads the recipient from the request body, so no extra headers are needed.
 */
@FeignClient(name = "notification-service", path = "/api/notifications")
public interface NotificationServiceClient {

    /**
     * Creates a notification for the given recipient.
     *
     * @param request the notification payload (userId, type, title, message, …)
     * @return the standard notification-service response
     */
    @PostMapping
    StandardResponse<Object> createNotification(@RequestBody NotificationCreateRequest request);
}
