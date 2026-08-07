package com.cloudnest.file.security;

/**
 * Outcome of a single virus scan.
 */
public enum ScanOutcome {
    /** No threats found. */
    CLEAN,
    /** A threat was detected — the file must be blocked. */
    INFECTED,
    /** The scanner could not complete (unreachable daemon, timeout, …). */
    ERROR
}
