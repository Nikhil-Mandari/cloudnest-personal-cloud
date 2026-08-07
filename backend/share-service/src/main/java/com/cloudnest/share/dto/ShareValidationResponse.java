package com.cloudnest.share.dto;

import com.cloudnest.share.entity.Share.ResourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of an internal share-token validation.
 * <p>
 * Used by the File Service to authorize share-link downloads without exposing
 * any internal share details. Password checks are performed by the Share
 * Service before the File Service is ever contacted.
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
    private ResourceType resourceType;

    /** Optional human-readable reason when not valid. */
    private String message;
}
