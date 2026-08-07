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
 * Access analytics for a single share, visible to the share owner only.
 * <p>
 * Includes lifetime view / download counters and the last-access timestamp
 * alongside the share configuration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShareAnalyticsResponse {

    private Long shareId;
    private String shareToken;
    private String resourceId;
    private ResourceType resourceType;
    private String resourceName;
    private Permission permission;
    private Boolean hasPassword;
    private Boolean isPublic;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private Long viewCount;
    private Long downloadCount;
    private LocalDateTime lastAccessedAt;
}
