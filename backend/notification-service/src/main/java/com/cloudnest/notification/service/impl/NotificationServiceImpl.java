package com.cloudnest.notification.service.impl;

import com.cloudnest.notification.dto.CreateNotificationRequest;
import com.cloudnest.notification.dto.NotificationResponse;
import com.cloudnest.notification.dto.UnreadCountResponse;
import com.cloudnest.notification.entity.Notification;
import com.cloudnest.notification.exception.NotificationNotFoundException;
import com.cloudnest.notification.mapper.NotificationMapper;
import com.cloudnest.notification.repository.NotificationRepository;
import com.cloudnest.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link NotificationService} interface.
 * <p>
 * Handles all notification management operations including creating,
 * retrieving, marking as read/unread, and deleting notifications.
 */
@Slf4j
@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Notification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new notification.
     */
    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.debug("Creating notification: userId={}, type={}, title='{}'",
                request.getUserId(), request.getType(), request.getTitle());

        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedResourceId(request.getRelatedResourceId())
                .relatedResourceType(request.getRelatedResourceType())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created: id={}, userId={}, type={}",
                saved.getId(), saved.getUserId(), saved.getType());

        return notificationMapper.toNotificationResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get User Notifications
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all notifications for a specific user, ordered by creation date descending.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        log.debug("Fetching notifications for userId={}", userId);

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Unread Notifications
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves all unread notifications for a specific user.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        log.debug("Fetching unread notifications for userId={}", userId);

        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Unread Count
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gets the count of unread notifications for a specific user.
     */
    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        log.debug("Counting unread notifications for userId={}", userId);

        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        log.debug("Unread notification count for userId={}: {}", userId, count);

        return UnreadCountResponse.builder()
                .count(count)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark as Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marks a single notification as read.
     * Validates that the notification belongs to the specified user.
     */
    @Override
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        log.debug("Marking notification as read: notificationId={}, userId={}",
                notificationId, userId);

        Notification notification = findNotificationByIdAndUser(notificationId, userId);

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            Notification saved = notificationRepository.save(notification);
            log.info("Notification marked as read: id={}", notificationId);
            return notificationMapper.toNotificationResponse(saved);
        }

        log.debug("Notification was already read: id={}", notificationId);
        return notificationMapper.toNotificationResponse(notification);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mark All as Read
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Marks all unread notifications as read for a specific user.
     */
    @Override
    public void markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for userId={}", userId);

        int updated = notificationRepository.markAllAsRead(userId);
        log.info("Marked {} notifications as read for userId={}", updated, userId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Notification
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a single notification.
     * Validates that the notification belongs to the specified user.
     */
    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        log.debug("Deleting notification: notificationId={}, userId={}",
                notificationId, userId);

        Notification notification = findNotificationByIdAndUser(notificationId, userId);

        notificationRepository.delete(notification);
        log.info("Notification deleted: id={}", notificationId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Internal helper to find a notification by ID and validate ownership,
     * or throw {@link NotificationNotFoundException}.
     *
     * @param notificationId the notification ID
     * @param userId         the expected owner user ID
     * @return the found Notification entity
     * @throws NotificationNotFoundException if no record exists or user doesn't own it
     */
    private Notification findNotificationByIdAndUser(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("Notification not found: id={}", notificationId);
                    return new NotificationNotFoundException(
                            "Notification not found with id: " + notificationId);
                });

        if (!notification.getUserId().equals(userId)) {
            log.warn("Notification access denied: id={}, userId={}, ownerId={}",
                    notificationId, userId, notification.getUserId());
            throw new NotificationNotFoundException(
                    "Notification not found with id: " + notificationId);
        }

        return notification;
    }
}
