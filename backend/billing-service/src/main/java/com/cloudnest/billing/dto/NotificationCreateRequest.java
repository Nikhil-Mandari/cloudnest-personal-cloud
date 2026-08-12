package com.cloudnest.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating a notification via the Notification Service (Feign).
 * Mirrors {@code CreateNotificationRequest} in notification-service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    private Long userId;

    private String type;

    private String title;

    private String message;

    private String relatedResourceId;

    private String relatedResourceType;
}
