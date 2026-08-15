package com.cloudnest.file.client;

import com.cloudnest.file.dto.QuotaResponse;
import com.cloudnest.file.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * OpenFeign client for the Billing Service.
 * <p>
 * Used to resolve the storage quota granted by the user's current
 * subscription so the File Service can enforce upload limits.
 */
@FeignClient(name = "billing-service", path = "/api/billing")
public interface BillingServiceClient {

    /**
     * Resolves the storage quota (bytes) for the given user.
     *
     * @param ownerId the authenticated user's ID (forwarded as X-User-Id)
     * @return the standard response containing the quota
     */
    @GetMapping("/quota")
    StandardResponse<QuotaResponse> getQuota(@RequestHeader("X-User-Id") Long ownerId);
}
