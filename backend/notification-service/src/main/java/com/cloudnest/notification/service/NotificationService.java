package com.cloudnest.notification.service;

import com.cloudnest.notification.dto.CreateNotificationRequest;
import com.cloudnest.notification.dto.NotificationResponse;
import com.cloudnest.notification.dto.UnreadCountResponse;

import java.util.List;

/**
 * Service interface for notification management operations.
 * <p>
 * Defines the contract for creating, retrieving, marking as read,
 * and deleting notifications for users.
 */
public interface NotificationService {

    /**
     * Creates a new notification.
     *
     * @param request the notification creation payload
     * @return the created notification response
     */
    NotificationResponse createNotification(CreateNotificationRequest request);

    /**
     * Retrieves all notifications for a specific user, ordered by creation date descending.
     *
     * @param userId the ID of the recipient user
     * @return a list of notification responses
     */
    List<NotificationResponse> getUserNotifications(Long userId);

    /**
     * Retrieves all unread notifications for a specific user.
     *
     * @param userId the ID of the recipient user
     * @return a list of unread notification responses
     */
    List<NotificationResponse> getUnreadNotifications(Long userId);

    /**
     * Gets the count of unread notifications for a specific user.
     *
     * @param userId the ID of the recipient user
     * @return the unread count response
     */
    UnreadCountResponse getUnreadCount(Long userId);

    /**
     * Marks a single notification as read.
     *
     * @param notificationId the ID of the notification to mark as read
     * @param userId         the ID of the recipient user (for ownership validation)
     * @return the updated notification response
     */
    NotificationResponse markAsRead(Long notificationId, Long userId);

    /**
     * Marks all unread notifications as read for a specific user.
     *
     * @param userId the ID of the recipient user
     */
    void markAllAsRead(Long userId);

    /**
     * Deletes a single notification.
     *
     * @param notificationId the ID of the notification to delete
     * @param userId         the ID of the recipient user (for ownership validation)
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * Deletes all read notifications for a specific user.
     *
     * @param userId the ID of the recipient user
     */
    void deleteAllRead(Long userId);
}
