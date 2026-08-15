package com.cloudnest.auth.service.impl;

import com.cloudnest.auth.client.UserServiceClient;
import com.cloudnest.auth.config.OAuthProperties;
import com.cloudnest.auth.dto.CreateProfileRequest;
import com.cloudnest.auth.entity.RefreshToken;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.DuplicateResourceException;
import com.cloudnest.auth.jwt.JwtProvider;
import com.cloudnest.auth.repository.RefreshTokenRepository;
import com.cloudnest.auth.repository.UserCredentialRepository;
import com.cloudnest.auth.service.OAuthService;
import com.cloudnest.auth.service.OAuthStateStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * OAuth2 authorization-code implementation for Google and GitHub.
 * <p>
 * Uses the provider's own HTTP APIs (no heavyweight Spring OAuth stack): the
 * browser is redirected to the provider, the provider calls back with a code,
 * the code is exchanged for an access token, and the user's identity is
 * resolved from the provider's userinfo endpoint. The resulting CloudNest
 * account is issued the standard JWT + refresh-token pair so social sign-in
 * reuses the existing session machinery end-to-end.
 * <p>
 * Only the database writes (user creation, refresh-token persistence) run
 * inside a transaction; the provider HTTP calls and the best-effort profile
 * provisioning run outside it so no DB connection is held across network I/O.
 */
@Slf4j
@Service
public class OAuthServiceImpl implements OAuthService {

    private final OAuthProperties properties;
    private final OAuthStateStore stateStore;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserCredentialRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserServiceClient userServiceClient;
    private final TransactionTemplate transactionTemplate;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Refresh tokens issued for social accounts live for the same 7 days as email/password logins. */
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 7L * 24 * 60 * 60;

    public OAuthServiceImpl(OAuthProperties properties,
                            OAuthStateStore stateStore,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            UserCredentialRepository userRepository,
                            RefreshTokenRepository refreshTokenRepository,
                            PasswordEncoder passwordEncoder,
                            JwtProvider jwtProvider,
                            UserServiceClient userServiceClient,
                            PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.stateStore = stateStore;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.userServiceClient = userServiceClient;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public boolean isConfigured(String provider) {
        OAuthProperties.Provider config = properties.provider(provider);
        return config != null && config.isConfigured();
    }

    @Override
    public String buildAuthorizeUrl(String provider) {
        OAuthProperties.Provider config = requireConfigured(provider);

        String state = stateStore.createState(provider);
        String url = config.getAuthorizeUrl()
                + (config.getAuthorizeUrl().contains("?") ? "&" : "?")
                + "client_id=" + encode(config.getClientId())
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&response_type=code"
                + "&scope=" + encode(config.getScope())
                + "&state=" + encode(state);

        log.info("OAuth authorize URL built for provider={}", provider);
        return url;
    }

    @Override
    public String handleCallback(String provider, String code, String state) {
        OAuthProperties.Provider config = requireConfigured(provider);

        // -- Validate the CSRF state (single-use) ------------------------------
        String stateProvider = stateStore.consume(state)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired OAuth state"));
        if (!stateProvider.equalsIgnoreCase(provider)) {
            throw new IllegalArgumentException("OAuth state does not match the provider");
        }

        // -- Exchange the authorization code for an access token --------------
        String accessToken = exchangeCode(config, code);

        // -- Resolve the provider user (network calls — no DB transaction) ----
        JsonNode userInfo = fetchUserInfo(config, accessToken);
        String email = extractVerifiedEmail(provider, userInfo, accessToken);
        String displayName = userInfo.path("name").asText(null);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("The provider did not return a verified email address for this account");
        }

        // -- Find or create the CloudNest account (transactional) --------------
        UserCredential user = transactionTemplate.execute(status ->
                findOrCreateUser(provider, email, displayName));

        // -- Issue the standard CloudNest session (transactional) --------------
        String refreshToken = transactionTemplate.execute(status -> issueRefreshToken(user.getId()));
        String token = jwtProvider.generateToken(user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        // Best-effort profile provisioning (idempotent in user-service), outside
        // the transaction — registration must not fail on user-service outage.
        provisionProfile(user);

        log.info("OAuth sign-in completed: provider={}, userId={}, username={}", provider, user.getId(), user.getUsername());

        // Tokens travel in the URL fragment (#) — never in the query string —
        // so they are not sent to servers or leaked via browser history/referer.
        return properties.getFrontendBaseUrl() + "/oauth/callback"
                + "#token=" + encode(token)
                + "&refreshToken=" + encode(refreshToken);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private OAuthProperties.Provider requireConfigured(String provider) {
        OAuthProperties.Provider config = properties.provider(provider);
        if (config == null || !config.isConfigured()) {
            throw new IllegalArgumentException("OAuth provider '" + provider + "' is not configured");
        }
        return config;
    }

    private String exchangeCode(OAuthProperties.Provider config, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // GitHub returns form-encoded by default; ask for JSON explicitly.
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", config.getClientId());
        body.add("client_secret", config.getClientSecret());
        body.add("code", code);
        body.add("grant_type", "authorization_code");
        body.add("redirect_uri", config.getRedirectUri());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                config.getTokenUrl(), HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

        JsonNode node = response.getBody();
        if (node == null || !node.has("access_token")) {
            throw new IllegalArgumentException("Token exchange failed: " + (node != null ? node : "empty response"));
        }
        return node.path("access_token").asText();
    }

    private JsonNode fetchUserInfo(OAuthProperties.Provider config, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                config.getUserInfoUrl(), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        if (response.getBody() == null) {
            throw new IllegalArgumentException("Provider userinfo returned an empty response");
        }
        return response.getBody();
    }

    /**
     * Resolves a <em>verified</em> email from the provider identity.
     * <ul>
     *   <li>Google: requires {@code email_verified == true} (an unverified
     *       address is rejected so it cannot be used to claim an account).</li>
     *   <li>GitHub: prefers the verified primary address from
     *       {@code /user/emails}; the public {@code userinfo.email} is only
     *       used when no verified email is available.</li>
     * </ul>
     */
    private String extractVerifiedEmail(String provider, JsonNode userInfo, String accessToken) {
        boolean google = "google".equalsIgnoreCase(provider);
        boolean github = "github".equalsIgnoreCase(provider);

        if (google) {
            boolean verified = userInfo.path("email_verified").asBoolean(false);
            String email = userInfo.path("email").asText(null);
            if (email != null && !email.isBlank() && verified) {
                return email.toLowerCase(Locale.ROOT);
            }
            return null;
        }

        if (github) {
            // Prefer the verified primary email from /user/emails.
            try {
                JsonNode emails = fetchGithubEmails(accessToken);
                for (JsonNode entry : emails) {
                    if (entry.path("primary").asBoolean(false) && entry.path("verified").asBoolean(false)) {
                        String primary = entry.path("email").asText(null);
                        if (primary != null && !primary.isBlank()) {
                            return primary.toLowerCase(Locale.ROOT);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not resolve verified GitHub email from /user/emails: {}", e.getMessage());
            }
            // Fall back to the public email only if present.
            String email = userInfo.path("email").asText(null);
            if (email != null && !email.isBlank()) {
                return email.toLowerCase(Locale.ROOT);
            }
            return null;
        }

        // Unknown provider — accept the email as-is.
        String email = userInfo.path("email").asText(null);
        return email == null || email.isBlank() ? null : email.toLowerCase(Locale.ROOT);
    }

    private JsonNode fetchGithubEmails(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                properties.getGithub().getEmailsUrl(),
                HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        return response.getBody() != null ? response.getBody() : objectMapper.createArrayNode();
    }

    /** Transactional: finds the account by email or creates it with an unusable password. */
    private UserCredential findOrCreateUser(String provider, String email, String displayName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createSocialUser(provider, email, displayName));
    }

    private UserCredential createSocialUser(String provider, String email, String displayName) {
        String baseUsername = baseUsername(email, displayName);
        String username = uniqueUsername(baseUsername);
        String unusablePassword = passwordEncoder.encode(randomHex(32));

        UserCredential user = UserCredential.builder()
                .username(username)
                .email(email)
                .password(unusablePassword)
                .role("ROLE_USER")
                .enabled(true)
                .build();

        try {
            user = userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Raced with another callback for the same email — the lookup above
            // should have caught it, but recover by re-reading the winning row.
            log.warn("Social user creation raced for email={}, re-reading existing account", email);
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new DuplicateResourceException(
                            "An account with that email already exists: " + email));
        }

        log.info("Social account created: provider={}, userId={}, username={}", provider, user.getId(), user.getUsername());
        return user;
    }

    /** Builds a clean username from the provider identity (max 50 chars, safe alphabet). */
    private String baseUsername(String email, String displayName) {
        String candidate = "";
        if (displayName != null && !displayName.isBlank()) {
            candidate = displayName;
        }
        if (candidate.isBlank() && email != null) {
            candidate = email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length());
        }
        String sanitized = candidate.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]", "")
                .replaceAll("^[^a-z0-9]+", "");
        if (sanitized.length() < 3) {
            sanitized = "user" + randomHex(4).substring(0, 4);
        }
        if (sanitized.length() > 45) {
            sanitized = sanitized.substring(0, 45);
        }
        return sanitized;
    }

    /** Ensures the username is unique by appending a short random suffix when needed. */
    private String uniqueUsername(String base) {
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        String candidate;
        int attempts = 0;
        do {
            candidate = base + "." + randomHex(3).substring(0, 3);
            attempts++;
        } while (userRepository.existsByUsername(candidate) && attempts < 10);
        return candidate;
    }

    /** Transactional: issues an opaque refresh token (only its SHA-256 hash is persisted). */
    private String issueRefreshToken(Long userId) {
        String rawToken = randomHex(32);

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    private void provisionProfile(UserCredential user) {
        try {
            CreateProfileRequest profile = CreateProfileRequest.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .role(user.getRole())
                    .build();
            userServiceClient.createProfile(profile);
            log.info("User profile provisioned via user-service: userId={}", user.getId());
        } catch (Exception e) {
            log.warn("Profile provisioning skipped/failed for userId={} — OAuth sign-in continues: {}",
                    user.getId(), e.getMessage());
        }
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
