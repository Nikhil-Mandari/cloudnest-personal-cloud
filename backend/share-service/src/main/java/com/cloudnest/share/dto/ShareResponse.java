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
    /** Display name of the shared resource (file name / folder name). */
    private String resourceName;
    private Long ownerId;
    private Long sharedWithUserId;
    private Permission permission;
    private String shareToken;
    private Boolean isPublic;
    /** Whether the share link is protected by a password (the hash itself is never exposed). */
    private Boolean hasPassword;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
}
