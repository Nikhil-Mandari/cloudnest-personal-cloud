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
 * Request DTO for updating a share's permission, expiry and/or link password.
 * <p>
 * Only the share owner may update a share. The password is stored as a
 * SHA-256 hash — the plain-text value is used to compute the hash and is
 * never persisted or returned.
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
     * True removes the expiry so the link never expires.
     */
    private Boolean clearExpiry;

    /**
     * Optional new link password. A non-blank value replaces the current
     * password hash; null/blank means no change.
     */
    private String password;

    /**
     * True removes password protection from the link.
     */
    private Boolean clearPassword;
}
