package com.cloudnest.share.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating a notification in the Notification Service via Feign.
 * <p>
 * Mirrors {@code CreateNotificationRequest} in the notification-service: the
 * recipient is {@code userId}, the event is identified by {@code type}
 * (e.g. {@code SHARE_RECEIVED}), and {@code relatedResourceId/Type} link the
 * notification back to the shared resource.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationCreateRequest {

    /** Recipient (the user the resource was shared with). */
    private Long userId;

    /** Notification type, e.g. {@code SHARE_RECEIVED}. */
    private String type;

    private String title;

    private String message;

    /** Optional ID of the related resource (file / folder). */
    private String relatedResourceId;

    /** Optional type of the related resource (FILE or FOLDER). */
    private String relatedResourceType;
}
