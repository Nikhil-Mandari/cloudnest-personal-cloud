package com.cloudnest.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Storage quota granted by the user's billing subscription.
 * <p>
 * Mirrors {@code QuotaResponse} in billing-service for Feign deserialization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaResponse {

    /** Plan name (FREE / PLUS / PRO / PREMIUM). */
    private String planType;

    /** Maximum allowed storage in bytes. */
    private Long quotaBytes;
}
