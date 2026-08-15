package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Enabling 2FA returns the freshly generated backup codes exactly once. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnableTwoFactorResponse {

    private boolean enabled;
    private List<String> backupCodes;
}
