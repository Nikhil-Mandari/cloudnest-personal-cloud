package com.cloudnest.notification.controller;

import com.cloudnest.notification.dto.CreateNotificationRequest;
import com.cloudnest.notification.dto.NotificationResponse;
import com.cloudnest.notification.dto.UnreadCountResponse;
import com.cloudnest.notification.service.NotificationService;
import com.cloudnest.notification.util.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for notification management operations.
 * <p>
 * Provides endpoints for creating, listing, marking as read, and deleting
 * in-app notifications. The authenticated user's ID is received via the
 * {@code X-User-Id} header, which is set by the API Gateway after JWT
 * authentication.
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Creates a new notification.
     * <p>
     * This endpoint is typically called internally by other services (via Feign).
     *
     * @param request     the notification creation payload
     * @param httpRequest the current HTTP request (for building response path)
     * @return 201 Created with the newly created notification
     */
    @PostMapping
    public ResponseEntity<StandardResponse<NotificationResponse>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            HttpServletRequest httpRequest) {

        log.info("POST /api/notifications - userId={}, type={}, title='{}'",
                request.getUserId(), request.getType(), request.getTitle());

        NotificationResponse response = notificationService.createNotification(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(StandardResponse.<NotificationResponse>builder()
                        .success(true)
                        .message("Notification created successfully")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves all notifications for the authenticated user,
     * ordered by creation date descending.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of notification responses
     */
    @GetMapping
    public ResponseEntity<StandardResponse<List<NotificationResponse>>> getUserNotifications(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/notifications - userId={}", userIdHeader);

        List<NotificationResponse> notifications =
                notificationService.getUserNotifications(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<NotificationResponse>>builder()
                        .success(true)
                        .message("Notifications retrieved successfully")
                        .data(notifications)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Retrieves only unread notifications for the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with a list of unread notification responses
     */
    @GetMapping("/unread")
    public ResponseEntity<StandardResponse<List<NotificationResponse>>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/notifications/unread - userId={}", userIdHeader);

        List<NotificationResponse> notifications =
                notificationService.getUnreadNotifications(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<List<NotificationResponse>>builder()
                        .success(true)
                        .message("Unread notifications retrieved successfully")
                        .data(notifications)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Gets the count of unread notifications for the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the unread count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<StandardResponse<UnreadCountResponse>> getUnreadCount(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("GET /api/notifications/unread-count - userId={}", userIdHeader);

        UnreadCountResponse count = notificationService.getUnreadCount(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<UnreadCountResponse>builder()
                        .success(true)
                        .message("Unread count retrieved successfully")
                        .data(count)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Marks a single notification as read.
     *
     * @param id           the ID of the notification to mark as read
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK with the updated notification
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<StandardResponse<NotificationResponse>> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/notifications/{}/read - userId={}", id, userIdHeader);

        NotificationResponse response = notificationService.markAsRead(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<NotificationResponse>builder()
                        .success(true)
                        .message("Notification marked as read")
                        .data(response)
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Marks all unread notifications as read for the authenticated user.
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the operation
     */
    @PutMapping("/read-all")
    public ResponseEntity<StandardResponse<Void>> markAllAsRead(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("PUT /api/notifications/read-all - userId={}", userIdHeader);

        notificationService.markAllAsRead(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("All notifications marked as read")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Deletes a single notification.
     *
     * @param id           the ID of the notification to delete
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<StandardResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/notifications/{} - userId={}", id, userIdHeader);

        notificationService.deleteNotification(id, userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Notification deleted successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }

    /**
     * Deletes all read notifications for the authenticated user ("clear").
     *
     * @param userIdHeader the authenticated user's ID (set by API Gateway from JWT)
     * @param httpRequest  the current HTTP request (for building response path)
     * @return 200 OK confirming the deletion
     */
    @DeleteMapping("/read-all")
    public ResponseEntity<StandardResponse<Void>> deleteAllRead(
            @RequestHeader("X-User-Id") Long userIdHeader,
            HttpServletRequest httpRequest) {

        log.info("DELETE /api/notifications/read-all - userId={}", userIdHeader);

        notificationService.deleteAllRead(userIdHeader);

        return ResponseEntity.ok(
                StandardResponse.<Void>builder()
                        .success(true)
                        .message("Read notifications cleared successfully")
                        .path(httpRequest.getRequestURI())
                        .build());
    }
}
