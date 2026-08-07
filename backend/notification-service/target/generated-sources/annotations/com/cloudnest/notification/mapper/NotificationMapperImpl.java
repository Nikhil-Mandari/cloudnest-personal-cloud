package com.cloudnest.notification.mapper;

import com.cloudnest.notification.dto.NotificationResponse;
import com.cloudnest.notification.entity.Notification;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-08T00:11:15+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationResponse toNotificationResponse(Notification notification) {
        if ( notification == null ) {
            return null;
        }

        NotificationResponse.NotificationResponseBuilder notificationResponse = NotificationResponse.builder();

        notificationResponse.id( notification.getId() );
        notificationResponse.userId( notification.getUserId() );
        notificationResponse.type( notification.getType() );
        notificationResponse.title( notification.getTitle() );
        notificationResponse.message( notification.getMessage() );
        notificationResponse.relatedResourceId( notification.getRelatedResourceId() );
        notificationResponse.relatedResourceType( notification.getRelatedResourceType() );
        notificationResponse.isRead( notification.getIsRead() );
        notificationResponse.createdAt( notification.getCreatedAt() );

        return notificationResponse.build();
    }
}
