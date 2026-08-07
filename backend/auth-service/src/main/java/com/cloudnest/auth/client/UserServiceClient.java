package com.cloudnest.auth.client;

import com.cloudnest.auth.config.UserServiceClientConfig;
import com.cloudnest.auth.dto.CreateUserProfileRequest;
import com.cloudnest.auth.dto.UserProfileResponse;
import com.cloudnest.auth.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for the User Service.
 * <p>
 * Used for provisioning user profiles (registration + admin bootstrap). The
 * configured request interceptor attaches the {@code ROLE_ADMIN} identity
 * headers, which the User Service requires for the profile-create endpoint.
 */
@FeignClient(name = "user-service", path = "/api/users", configuration = UserServiceClientConfig.class)
public interface UserServiceClient {

    /**
     * Creates (or returns the existing) user profile. Idempotent: if a
     * profile with the same username/email already exists, it is returned
     * unchanged.
     */
    @PostMapping
    StandardResponse<UserProfileResponse> createProfile(@RequestBody CreateUserProfileRequest request);
}
