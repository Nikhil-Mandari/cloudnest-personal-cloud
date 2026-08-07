package com.cloudnest.auth.config;

import com.cloudnest.auth.repository.PasskeyCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

/**
 * WebAuthn relying-party wiring for passkeys.
 * <p>
 * The {@code rpId} and allowed origins come from {@code auth.webauthn.*}
 * (config-repo/auth-service.yml). In development the relying party id is
 * {@code localhost} and the origin is the Vite dev server. Browsers treat
 * {@code http://localhost} as a secure context, which WebAuthn requires.
 */
@Slf4j
@Configuration
public class WebAuthnConfig {

    private final AuthProperties properties;
    private final PasskeyCredentialRepository credentialRepository;

    public WebAuthnConfig(AuthProperties properties,
                          PasskeyCredentialRepository credentialRepository) {
        this.properties = properties;
        this.credentialRepository = credentialRepository;
    }

    @Bean
    public RelyingParty relyingParty() {
        AuthProperties.Webauthn config = properties.getWebauthn();

        RelyingParty rp = RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(config.getRpId())
                        .name(config.getRpName())
                        .build())
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(config.getOrigins()))
                .build();

        log.info("WebAuthn configured — rpId='{}', origins={}", config.getRpId(), config.getOrigins());
        return rp;
    }
}
