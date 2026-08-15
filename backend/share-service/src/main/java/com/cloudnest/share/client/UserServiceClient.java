package com.cloudnest.share.client;

import com.cloudnest.share.dto.UserResponse;
import com.cloudnest.share.util.StandardResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * OpenFeign client for communicating with the User Service.
 * <p>
 * Used to validate that shared-with users exist in the system
 * and to look up users by email via the search endpoint.
 */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {

    /**
     * Retrieves a user by their internal ID.
     *
     * @param id the user's internal ID
     * @return the standard response containing user data
     */
    @GetMapping("/{id}")
    StandardResponse<UserResponse> getUserById(@PathVariable("id") Long id);

    /**
     * Searches for users by username, display name, or email.
     * Used to look up a user by email when sharing.
     *
     * @param query the search term (email address)
     * @return the standard response containing a list of matching users
     */
    @GetMapping
    StandardResponse<List<UserResponse>> searchUsers(@RequestParam("query") String query);
}
