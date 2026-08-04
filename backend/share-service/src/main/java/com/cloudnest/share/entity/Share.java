package com.cloudnest.share.entity;

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
 * Represents a share record in the CloudNest platform.
 * <p>
 * A share defines access permissions granted to a specific user (or publicly)
 * for a file or folder resource. Supports both user-to-user sharing and
 * public share links with expiry dates.
 */
@Entity
@Table(name = "shares")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Share {

    /**
     * Internal auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the resource being shared (file or folder).
     */
    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    /**
     * Type of the shared resource — either FILE or FOLDER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    /**
     * ID of the user who owns the shared resource.
     */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    /**
     * ID of the user with whom the resource is shared.
     * May be null for public shares.
     */
    @Column(name = "shared_with_user_id")
    private Long sharedWithUserId;

    /**
     * Permission level granted to the share recipient.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 20)
    private Permission permission;

    /**
     * Unique token for accessing the shared resource via a public link.
     * Generated as a UUID for secure public sharing.
     */
    @Column(name = "share_token", nullable = false, unique = true, length = 36)
    private String shareToken;

    /**
     * Whether this share is publicly accessible without authentication.
     */
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * Expiry date for the share. Null means the share never expires.
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Timestamp when the share record was created.
     * Managed automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Enum representing the type of resource being shared.
     */
    public enum ResourceType {
        FILE,
        FOLDER
    }

    /**
     * Enum representing the permission level granted to the share recipient.
     */
    public enum Permission {
        VIEW,
        EDIT
    }
}
