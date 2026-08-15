package com.cloudnest.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Represents an in-app notification in the CloudNest platform.
 * <p>
 * Notifications are created when share events occur (e.g. a file is shared
 * with a user), when system alerts need to be delivered, or when user
 * activity requires attention. Each notification targets a specific user
 * and can be marked as read.
 */
@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    /**
     * Internal auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the user who receives this notification.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Type of the notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    /**
     * Short title of the notification.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Detailed message body of the notification.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Optional ID of the related resource (file, folder, share, etc.).
     */
    @Column(name = "related_resource_id", length = 36)
    private String relatedResourceId;

    /**
     * Optional type of the related resource (FILE or FOLDER).
     */
    @Column(name = "related_resource_type", length = 20)
    private String relatedResourceType;

    /**
     * Whether this notification has been read by the user.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Timestamp when the notification was created.
     * Managed automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enum representing the possible types of notifications.
     */
    public enum NotificationType {
        /**
         * A file was shared with the user.
         */
        SHARE_RECEIVED,

        /**
         * A share's permission was updated.
         */
        SHARE_UPDATED,

        /**
         * A share was revoked.
         */
        SHARE_REVOKED,

        /**
         * A file was shared by the user with someone.
         */
        FILE_SHARED,

        /**
         * A folder was shared by the user with someone.
         */
        FOLDER_SHARED,

        /**
         * A payment for a plan upgrade succeeded.
         */
        PAYMENT_SUCCESS,

        /**
         * A payment failed or was rejected.
         */
        PAYMENT_FAILED,

        /**
         * The user's storage plan was upgraded.
         */
        PLAN_UPGRADED,

        /**
         * General system notification.
         */
        SYSTEM
    }
}
