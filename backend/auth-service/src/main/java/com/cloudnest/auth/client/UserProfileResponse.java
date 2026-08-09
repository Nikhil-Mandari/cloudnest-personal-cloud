package com.cloudnest.auth.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal user profile projection received from the User Service via Feign.
 * <p>
 * Only the fields the Auth Service needs are mapped (see the equivalent
 * consumer-side DTO in the Share Service).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String role;
    private Boolean enabled;
}
