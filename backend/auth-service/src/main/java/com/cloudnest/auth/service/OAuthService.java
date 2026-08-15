package com.cloudnest.auth.service;

/**
 * Social-login (OAuth2 authorization-code) orchestration.
 * <p>
 * The flow is driven by full-page redirects:
 * <ol>
 *   <li>{@code GET /api/auth/oauth/{provider}/authorize} → 302 to the provider's
 *       consent screen (with a CSRF {@code state}).</li>
 *   <li>The provider redirects back to {@code /api/auth/oauth/{provider}/callback}.</li>
 *   <li>The callback exchanges the code for an access token, fetches the user's
 *       profile, finds-or-creates the CloudNest account, issues a JWT + refresh
 *       token, and finally 302s the browser to the frontend callback page.</li>
 * </ol>
 */
public interface OAuthService {

    /** Whether the provider has valid client credentials configured. */
    boolean isConfigured(String provider);

    /**
     * Builds the provider authorization URL (already containing the CSRF state).
     *
     * @param provider provider name (google / github)
     * @return the full authorization URL
     */
    String buildAuthorizeUrl(String provider);

    /**
     * Completes the OAuth callback: validates state, exchanges the code,
     * resolves or creates the user, issues the CloudNest session, and returns
     * the frontend redirect URL carrying the tokens.
     *
     * @param provider provider name (google / github)
     * @param code     the authorization code from the provider
     * @param state    the CSRF state echoed back by the provider
     * @return the frontend callback URL with {@code token}/{@code refreshToken} query params
     */
    String handleCallback(String provider, String code, String state);
}
