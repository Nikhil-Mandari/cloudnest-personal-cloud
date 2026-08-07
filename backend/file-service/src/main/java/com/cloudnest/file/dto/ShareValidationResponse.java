package com.cloudnest.file.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of a share-token validation received from the Share Service.
 * <p>
 * Mirrors the Share Service's validation payload so the File Service can
 * authorize share-link downloads without owning any share data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShareValidationResponse {

    /** Whether the token resolves to a live, unexpired share covering the resource. */
    private boolean valid;

    /** The resource covered by the share (only set when valid). */
    private String resourceId;

    /** The type of the shared resource (only set when valid). */
    private String resourceType;

    /** Optional human-readable reason when not valid. */
    private String message;
}
