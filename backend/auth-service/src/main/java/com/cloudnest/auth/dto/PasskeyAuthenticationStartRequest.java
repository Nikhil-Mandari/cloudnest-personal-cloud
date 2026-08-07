package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Passkey sign-in start: an optional username. The current implementation
 * runs discovery-less assertions (the browser offers every discoverable
 * credential for the relying party), so this field is accepted for API
 * forward-compatibility and is not yet used for scoping.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationStartRequest {

    private String username;
}
