package com.cloudnest.billing.client;

import com.cloudnest.billing.dto.NotificationCreateRequest;
import com.cloudnest.billing.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign client for the Notification Service.
 * <p>
 * Used to notify the user about payment success / failure and plan
 * upgrades. The create endpoint reads the recipient from the request body,
 * so no extra headers are needed.
 */
@FeignClient(name = "notification-service", path = "/api/notifications")
public interface NotificationServiceClient {

    @PostMapping
    StandardResponse<Object> createNotification(@RequestBody NotificationCreateRequest request);
}
