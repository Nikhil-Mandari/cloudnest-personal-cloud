package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.UserCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Brute-force protection: after {@code auth.lock.max-failed-attempts}
 * consecutive failed password attempts the account locks for
 * {@code auth.lock.duration-minutes}. A successful login resets the counter.
 */
@Slf4j
@Service
public class AccountLockService {

    private final AuthProperties properties;

    public AccountLockService(AuthProperties properties) {
        this.properties = properties;
    }

    /**
     * @return {@code true} when the account is currently locked.
     */
    public boolean isLocked(UserCredential user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    /**
     * Minutes remaining until the lock lifts.
     */
    public long remainingMinutes(UserCredential user) {
        if (user.getLockedUntil() == null) {
            return 0;
        }
        long seconds = Duration.between(LocalDateTime.now(), user.getLockedUntil()).getSeconds();
        return Math.max(0, (seconds + 59) / 60);
    }

    /**
     * Records a failed attempt. When the threshold is reached the account is
     * locked and {@code status} flips to {@code LOCKED}.
     *
     * @return {@code true} when the account became locked as a result
     */
    @Transactional
    public boolean registerFailure(UserCredential user) {
        int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
        user.setFailedAttempts(attempts + 1);

        int max = properties.getLock().getMaxFailedAttempts();
        if (user.getFailedAttempts() >= max) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(properties.getLock().getDurationMinutes()));
            user.setStatus(UserCredential.AccountStatus.LOCKED);
            log.warn("Account userId={} locked for {} minutes after {} failed attempts",
                    user.getId(), properties.getLock().getDurationMinutes(), user.getFailedAttempts());
            return true;
        }
        log.debug("Failed attempt {} of {} for userId={}", user.getFailedAttempts(), max, user.getId());
        return false;
    }

    /**
     * Resets the failure counter and clears any lock after a successful login
     * (or an explicit unlock).
     */
    @Transactional
    public void reset(UserCredential user) {
        boolean wasLocked = isLocked(user) || UserCredential.AccountStatus.LOCKED.equals(user.getStatus());
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        if (UserCredential.AccountStatus.LOCKED.equals(user.getStatus())) {
            user.setStatus(UserCredential.AccountStatus.ACTIVE);
        }
        if (wasLocked) {
            log.info("Account userId={} unlocked", user.getId());
        }
    }
}
