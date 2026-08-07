package com.cloudnest.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating a user profile.
 * <p>
 * All fields are optional — only provided fields will be updated.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;

    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Size(max = 512, message = "Avatar URL must not exceed 512 characters")
    private String avatarUrl;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phone;
}
