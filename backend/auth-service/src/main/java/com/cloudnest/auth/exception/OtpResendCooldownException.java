package com.cloudnest.auth.exception;

/**
 * Thrown when a new code is requested before the resend cooldown elapses.
 */
public class OtpResendCooldownException extends OtpException {

    /** Seconds remaining until the next resend is allowed. */
    private final long remainingSeconds;

    public OtpResendCooldownException(long remainingSeconds) {
        super("Please wait " + remainingSeconds + " seconds before requesting a new code.");
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
