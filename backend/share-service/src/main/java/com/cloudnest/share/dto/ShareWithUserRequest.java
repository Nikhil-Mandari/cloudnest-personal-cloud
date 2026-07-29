package com.cloudnest.share.dto;

import com.cloudnest.share.entity.Share.Permission;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request DTO for sharing a resource with a specific user.
 * <p>
 * The recipient can be identified by user ID or email.
 * The permission level and optional expiry date are configurable.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareWithUserRequest {

    /**
     * ID of the user to share with (optional if email is provided).
     */
    private Long sharedWithUserId;

    /**
     * Email of the user to share with (optional if userId is provided).
     */
    private String sharedWithEmail;

    @NotNull(message = "Permission must not be null")
    private Permission permission;

    /**
     * Optional expiry date for the share. Null means never expires.
     */
    private LocalDateTime expiryDate;
}
