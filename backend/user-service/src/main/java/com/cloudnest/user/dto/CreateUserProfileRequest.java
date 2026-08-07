package com.cloudnest.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for provisioning a user profile. Called by the Auth Service after
 * registration and by the admin bootstrap. Idempotent: an existing profile
 * with the same email/username is returned unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserProfileRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 100, message = "Display name must be at most 100 characters")
    private String displayName;

    /** Role as stored (ROLE_USER / ROLE_ADMIN); defaults to ROLE_USER. */
    private String role;

    /** Defaults to {@code true}. */
    private Boolean enabled;
}
