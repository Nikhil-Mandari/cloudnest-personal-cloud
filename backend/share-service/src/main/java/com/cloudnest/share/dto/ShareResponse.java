package com.cloudnest.share.dto;

import com.cloudnest.share.entity.Share.Permission;
import com.cloudnest.share.entity.Share.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO containing all share properties.
 * <p>
 * Returned for all share operations including creation, retrieval,
 * permission updates, and listing.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShareResponse {

    private Long id;
    private String resourceId;
    private ResourceType resourceType;

    /**
     * Display name of the shared resource (file name or folder name), resolved
     * from the owning service. Null when the resource could not be resolved
     * (omitted from JSON via {@code @JsonInclude(NON_NULL)}).
     */
    private String resourceName;
    private Long ownerId;
    private Long sharedWithUserId;
    private Permission permission;
    private String shareToken;
    private Boolean isPublic;
    private LocalDateTime expiryDate;

    /**
     * Whether this share link is protected by a password.
     * The hash itself is never exposed.
     */
    private Boolean hasPassword;

    /**
     * Number of times the share link was viewed.
     */
    private Long viewCount;

    /**
     * Number of times the shared resource was downloaded through this link.
     */
    private Long downloadCount;

    /**
     * Timestamp of the most recent access to this share.
     */
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
}
