package com.cloudnest.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for social login (Google / GitHub OAuth2 authorization-code flow).
 * <p>
 * All values are resolved from environment variables (see {@code config-repo/auth-service.yml}).
 * A provider is considered configured only when client ID, client secret and
 * redirect URI are all present. When a provider is not configured, its
 * authorize endpoint responds with HTTP 503 so the frontend can hide/disable
 * the button instead of showing a broken redirect.
 */
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    /** Where to redirect the browser after a successful social sign-in. */
    private String frontendBaseUrl = "http://localhost:5173";

    private Provider google = new Provider();
    private Provider github = new Provider();

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getGithub() {
        return github;
    }

    public void setGithub(Provider github) {
        this.github = github;
    }

    /**
     * Convenience lookup by provider name ({@code google} / {@code github}).
     */
    public Provider provider(String name) {
        return switch (name == null ? "" : name.toLowerCase()) {
            case "google" -> google;
            case "github" -> github;
            default -> null;
        };
    }

    /** Per-provider OAuth2 settings. */
    public static class Provider {

        private String clientId = "";
        private String clientSecret = "";
        private String redirectUri = "";
        private String authorizeUrl = "";
        private String tokenUrl = "";
        private String userInfoUrl = "";
        /** GitHub-only: endpoint listing the account's emails (verification status). */
        private String emailsUrl = "https://api.github.com/user/emails";
        private String scope = "";

        /** A provider is usable only when all three credentials are present. */
        public boolean isConfigured() {
            return !clientId.isBlank() && !clientSecret.isBlank() && !redirectUri.isBlank();
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public void setAuthorizeUrl(String authorizeUrl) {
            this.authorizeUrl = authorizeUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getEmailsUrl() {
            return emailsUrl;
        }

        public void setEmailsUrl(String emailsUrl) {
            this.emailsUrl = emailsUrl;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }
    }
}
