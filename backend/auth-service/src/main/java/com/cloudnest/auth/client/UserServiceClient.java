package com.cloudnest.auth.client;

import com.cloudnest.auth.dto.CreateProfileRequest;
import com.cloudnest.auth.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenFeign client for communicating with the User Service.
 * <p>
 * Used to provision the user profile in {@code user_db} after a successful
 * registration in {@code auth_db}. The call is best-effort — registration
 * must not fail when the User Service is temporarily unavailable.
 */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {

    /**
     * Provisions a user profile (idempotent on the Auth user ID).
     *
     * @param request the profile data (auth user ID, username, email, role)
     * @return the standard response containing the profile
     */
    @PostMapping
    StandardResponse<UserProfileResponse> createProfile(@RequestBody CreateProfileRequest request);
}
