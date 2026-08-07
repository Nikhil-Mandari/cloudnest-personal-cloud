package com.cloudnest.auth.exception;

/**
 * Thrown when a sign-in is attempted while the account is temporarily locked
 * after too many failed attempts. Maps to HTTP 423 Locked.
 */
public class AccountLockedException extends RuntimeException {

    /** Minutes remaining until the account unlocks. */
    private final long remainingMinutes;

    public AccountLockedException(long remainingMinutes) {
        super("Account temporarily locked after too many failed attempts. "
                + "Try again in " + remainingMinutes + " minute(s).");
        this.remainingMinutes = remainingMinutes;
    }

    public long getRemainingMinutes() {
        return remainingMinutes;
    }
}
