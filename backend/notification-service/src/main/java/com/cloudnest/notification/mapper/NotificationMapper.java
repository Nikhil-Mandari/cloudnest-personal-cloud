package com.cloudnest.notification.mapper;

import com.cloudnest.notification.dto.NotificationResponse;
import com.cloudnest.notification.entity.Notification;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting between {@link Notification} entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    /**
     * Converts a {@link Notification} entity into a {@link NotificationResponse}.
     *
     * @param notification the notification entity (must not be null)
     * @return a populated {@link NotificationResponse}
     */
    NotificationResponse toNotificationResponse(Notification notification);
}
