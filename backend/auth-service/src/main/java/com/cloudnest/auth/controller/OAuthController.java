package com.cloudnest.auth.controller;

import com.cloudnest.auth.config.OAuthProperties;
import com.cloudnest.auth.service.OAuthService;
import com.cloudnest.auth.util.StandardResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Social-login endpoints (Google / GitHub).
 * <p>
 * Both endpoints are browser-driven full-page redirects:
 * <ul>
 *   <li>{@code GET /api/auth/oauth/{provider}/authorize} — 302 to the
 *       provider consent screen (503 when the provider is not configured).</li>
 *   <li>{@code GET /api/auth/oauth/{provider}/callback} — completes the flow
 *       and 302s to the frontend callback page with the session tokens
 *       (or to the login page with {@code ?oauth=error} on failure).</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/oauth")
public class OAuthController {

    private final OAuthService oauthService;
    private final OAuthProperties oauthProperties;

    public OAuthController(OAuthService oauthService, OAuthProperties oauthProperties) {
        this.oauthService = oauthService;
        this.oauthProperties = oauthProperties;
    }

    /**
     * Reports whether a provider is configured, so the frontend can hide
     * buttons for providers without client credentials.
     */
    @GetMapping("/{provider}/status")
    public StandardResponse<java.util.Map<String, Object>> status(@PathVariable String provider) {
        return StandardResponse.<java.util.Map<String, Object>>builder()
                .success(true)
                .message("OAuth provider status")
                .data(java.util.Map.of(
                        "provider", provider,
                        "configured", oauthService.isConfigured(provider)))
                .path("/api/auth/oauth/" + provider + "/status")
                .build();
    }

    /**
     * Starts the OAuth flow by redirecting the browser to the provider.
     */
    @GetMapping("/{provider}/authorize")
    public void authorize(@PathVariable String provider, HttpServletResponse response) throws IOException {
        log.info("GET /api/auth/oauth/{}/authorize", provider);

        if (!oauthService.isConfigured(provider)) {
            log.warn("OAuth provider '{}' is not configured — returning 503", provider);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "OAuth provider '" + provider + "' is not configured on the server");
            return;
        }

        response.sendRedirect(oauthService.buildAuthorizeUrl(provider));
    }

    /**
     * Receives the provider redirect, completes the flow, and sends the browser
     * to the frontend with the new session tokens.
     */
    @GetMapping("/{provider}/callback")
    public void callback(@PathVariable String provider,
                         @RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {

        log.info("GET /api/auth/oauth/{}/callback (codePresent={}, statePresent={})",
                provider, code != null && !code.isBlank(), state != null && !state.isBlank());

        try {
            String redirect = oauthService.handleCallback(provider, code, state);
            response.sendRedirect(redirect);
        } catch (Exception e) {
            log.warn("OAuth callback failed for provider={}: {}", provider, e.getMessage());
            response.sendRedirect(oauthProperties.getFrontendBaseUrl() + "/login?oauth=error");
        }
    }
}
