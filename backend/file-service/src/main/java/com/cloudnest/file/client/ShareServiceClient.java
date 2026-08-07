package com.cloudnest.file.client;

import com.cloudnest.file.dto.ShareValidationResponse;
import com.cloudnest.file.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign client for communicating with the Share Service.
 * <p>
 * Used to authorize share-link downloads: the File Service asks the Share
 * Service whether a given token covers a given resource before streaming the
 * content. The token is the capability — possession of a valid, unexpired
 * token for the resource authorizes the stream.
 */
@FeignClient(name = "share-service", path = "/api/shares")
public interface ShareServiceClient {

    /**
     * Validates a public share token against a resource ID.
     *
     * @param token      the public share token
     * @param resourceId the resource ID the token must cover (nullable)
     * @return the validation result wrapped in the standard response envelope
     */
    @GetMapping("/internal/validate")
    StandardResponse<ShareValidationResponse> validateShare(
            @RequestParam("token") String token,
            @RequestParam(value = "resourceId", required = false) String resourceId);
}
