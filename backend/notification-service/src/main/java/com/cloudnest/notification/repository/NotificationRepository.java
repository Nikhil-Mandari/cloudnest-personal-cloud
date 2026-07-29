package com.cloudnest.notification.repository;

import com.cloudnest.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Notification} entity operations.
 * <p>
 * Provides standard CRUD plus custom queries for notification management
 * including filtering by user, unread status, and bulk read operations.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Finds all notifications for a specific user, ordered by creation date descending.
     *
     * @param userId the ID of the recipient user
     * @return a list of notifications for the specified user
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Finds all unread notifications for a specific user, ordered by creation date descending.
     *
     * @param userId the ID of the recipient user
     * @return a list of unread notifications for the specified user
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Counts the number of unread notifications for a specific user.
     *
     * @param userId the ID of the recipient user
     * @return the count of unread notifications
     */
    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Marks all unread notifications as read for a specific user.
     *
     * @param userId the ID of the recipient user
     * @return the number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") Long userId);
}
