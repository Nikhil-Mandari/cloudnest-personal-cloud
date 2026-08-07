package com.cloudnest.notification.dto;

import com.cloudnest.notification.entity.Notification.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO containing all notification properties.
 * <p>
 * Returned for all notification operations including listing,
 * creating, and retrieving individual notifications.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String message;
    private String relatedResourceId;
    private String relatedResourceType;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
