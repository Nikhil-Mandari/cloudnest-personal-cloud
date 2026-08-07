package com.cloudnest.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Passkey sign-in start: an optional username. When supplied, the assertion
 * is restricted to that user's registered credentials (discoverable
 * credentials still work when omitted).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasskeyAuthenticationStartRequest {

    private String username;
}
