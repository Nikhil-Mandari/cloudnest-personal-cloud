package com.cloudnest.file.exception;

/**
 * Thrown when uploaded content is flagged as infected by the virus scanner.
 * The upload is aborted and the offending object is removed from storage.
 */
public class VirusDetectedException extends RuntimeException {

    public VirusDetectedException(String message) {
        super(message);
    }
}
