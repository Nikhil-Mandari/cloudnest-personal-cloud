package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for creating an in-app notification via the Notification Service.
 * <p>
 * {@code type} carries the wire-format name of the notification-service
 * {@code NotificationType} enum value (e.g. {@code LOGIN_ALERT}). Jackson
 * resolves it into the enum on the notification-service side, so this
 * service deliberately does not duplicate the enum — the accepted names
 * are documented as constants on {@code NotificationServiceClient}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    /** ID of the user who receives the notification. */
    private Long userId;

    /** Notification-service {@code NotificationType} name. */
    private String type;

    /** Short title, e.g. "New device sign-in". */
    private String title;

    /** Human-readable message body. */
    private String message;
}
