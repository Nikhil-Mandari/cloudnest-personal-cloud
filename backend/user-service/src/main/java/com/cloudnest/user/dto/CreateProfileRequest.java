package com.cloudnest.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for provisioning a user profile.
 * <p>
 * Called by the Auth Service after a successful registration. The {@code id}
 * is the Auth Service's own numeric user ID (from
 * {@code auth_db.user_credentials}) so that the same identifier is used as
 * the profile primary key in {@code user_db.users}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProfileRequest {

    @NotNull(message = "User ID is required")
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Role is required")
    @Size(max = 20, message = "Role must not exceed 20 characters")
    private String role;
}
