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
 * Request DTO for updating a share's permission level and/or expiry.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePermissionRequest {

    @NotNull(message = "Permission must not be null")
    private Permission permission;

    /**
     * Optional new expiry date. Null means no change.
     */
    private LocalDateTime expiryDate;

    /**
     * When {@code true}, removes any existing expiry so the link never expires.
     * Takes precedence over {@code expiryDate}.
     */
    private Boolean clearExpiry;

    /**
     * Optional new password for the share link. Null means no change.
     */
    private String password;

    /**
     * When {@code true}, removes any existing password protection.
     */
    private Boolean clearPassword;
}
