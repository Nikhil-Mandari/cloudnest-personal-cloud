package com.cloudnest.auth.service;

import com.cloudnest.auth.dto.AuthResponse;
import com.cloudnest.auth.dto.DeviceInfo;
import com.cloudnest.auth.dto.PasskeyAuthenticationFinish;
import com.cloudnest.auth.dto.PasskeyAuthenticationStart;
import com.cloudnest.auth.dto.PasskeyCredentialInfo;
import com.cloudnest.auth.dto.PasskeyRegistrationFinish;
import com.cloudnest.auth.dto.PasskeyRegistrationStart;

import java.util.List;

/**
 * WebAuthn (passkey) registration and authentication.
 */
public interface PasskeyService {

    /** Begins a registration ceremony for the signed-in user. */
    PasskeyRegistrationStart startRegistration(Long userId);

    /** Verifies and stores the browser credential. */
    PasskeyCredentialInfo finishRegistration(Long userId, PasskeyRegistrationFinish request);

    List<PasskeyCredentialInfo> listPasskeys(Long userId);

    void removePasskey(Long userId, String credentialId);

    /** Begins a (discoverable-credential) sign-in ceremony. */
    PasskeyAuthenticationStart startAuthentication();

    /** Verifies the assertion and issues a CloudNest session. */
    AuthResponse finishAuthentication(PasskeyAuthenticationFinish request, DeviceInfo device);
}
