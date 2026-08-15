package com.cloudnest.notification.dto;

import com.cloudnest.notification.entity.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating a new notification.
 * <p>
 * Used internally by other services (via Feign) or by the
 * notification service itself to generate notifications.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNotificationRequest {

    @NotNull(message = "User ID must not be null")
    private Long userId;

    @NotNull(message = "Notification type must not be null")
    private NotificationType type;

    @NotBlank(message = "Title must not be blank")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Message must not be blank")
    private String message;

    /**
     * Optional ID of the related resource (file, folder, share, etc.).
     */
    private String relatedResourceId;

    /**
     * Optional type of the related resource (FILE or FOLDER).
     */
    private String relatedResourceType;
}
