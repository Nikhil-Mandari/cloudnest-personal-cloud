package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.UserCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link AccountLockService}.
 */
class AccountLockServiceTest {

    private AuthProperties properties;
    private AccountLockService service;

    @BeforeEach
    void setUp() {
        properties = new AuthProperties();
        properties.getLock().setMaxFailedAttempts(5);
        properties.getLock().setDurationMinutes(15);
        service = new AccountLockService(properties);
    }

    @Test
    @DisplayName("Locks the account when the failure threshold is reached")
    void registerFailure_locksAtThreshold() {
        UserCredential user = UserCredential.builder().id(1L).failedAttempts(0).build();

        boolean locked = false;
        for (int i = 0; i < 5; i++) {
            locked = service.registerFailure(user);
        }

        assertTrue(locked);
        assertEquals(5, user.getFailedAttempts());
        assertEquals(UserCredential.AccountStatus.LOCKED, user.getStatus());
        assertTrue(service.isLocked(user));
        assertTrue(service.remainingMinutes(user) > 0);
    }

    @Test
    @DisplayName("Does not lock before the threshold")
    void registerFailure_belowThreshold() {
        UserCredential user = UserCredential.builder().id(1L).failedAttempts(0).build();

        for (int i = 0; i < 4; i++) {
            assertFalse(service.registerFailure(user));
        }
        assertFalse(service.isLocked(user));
        assertEquals(4, user.getFailedAttempts());
    }

    @Test
    @DisplayName("Successful login resets the counter and unlocks the account")
    void reset_clearsCounterAndLock() {
        UserCredential user = UserCredential.builder().id(1L).failedAttempts(5).build();
        user.setStatus(UserCredential.AccountStatus.LOCKED);
        user.setLockedUntil(java.time.LocalDateTime.now().plusMinutes(15));

        service.reset(user);

        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockedUntil());
        assertEquals(UserCredential.AccountStatus.ACTIVE, user.getStatus());
        assertFalse(service.isLocked(user));
    }

    @Test
    @DisplayName("A fresh account is not locked")
    void isLocked_freshAccount() {
        UserCredential user = UserCredential.builder().id(1L).failedAttempts(0).build();
        assertFalse(service.isLocked(user));
        assertEquals(0, service.remainingMinutes(user));
    }
}
