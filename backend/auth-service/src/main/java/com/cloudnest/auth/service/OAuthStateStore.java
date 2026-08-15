package com.cloudnest.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for OAuth2 {@code state} parameters.
 * <p>
 * The {@code state} value is a random UUID bound to the provider; it is sent
 * to the provider on the authorize request and verified on the callback,
 * which protects the flow against CSRF / login-stripping attacks. Entries
 * expire after {@link #STATE_TTL_SECONDS} and are consumed once.
 */
@Component
public class OAuthStateStore {

    /** How long an authorization attempt may stay pending. */
    private static final int STATE_TTL_SECONDS = 10 * 60;

    private final Map<String, PendingState> states = new ConcurrentHashMap<>();

    /**
     * Creates a fresh state token for the given provider.
     *
     * @param provider provider name (google / github)
     * @return the opaque state token
     */
    public String createState(String provider) {
        String state = UUID.randomUUID().toString();
        states.put(state, new PendingState(provider, Instant.now()));
        return state;
    }

    /**
     * Validates and consumes a state token, returning the provider it was
     * issued for. The entry is removed on lookup (single-use).
     *
     * @param state the state value received on the callback
     * @return the provider, or empty when unknown/expired
     */
    public Optional<String> consume(String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        PendingState pending = states.remove(state);
        if (pending == null) {
            return Optional.empty();
        }
        if (pending.createdAt().isBefore(Instant.now().minusSeconds(STATE_TTL_SECONDS))) {
            return Optional.empty();
        }
        return Optional.of(pending.provider());
    }

    /** Hourly sweep that drops abandoned (never-consumed) authorization attempts. */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupExpiredStates() {
        Instant cutoff = Instant.now().minusSeconds(STATE_TTL_SECONDS);
        states.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }

    /** A pending authorization attempt. */
    private record PendingState(String provider, Instant createdAt) {
    }
}
