package com.cloudnest.auth.service;

/**
 * Service for sending transactional emails (OTP codes, notifications).
 */
public interface EmailService {

    /**
     * Sends an OTP verification email to the given address.
     *
     * @param to           the recipient email address
     * @param otpCode      the 6-digit OTP code
     * @param expiryMinutes how many minutes the OTP remains valid
     * @throws EmailService.EmailSendException when email delivery fails
     */
    void sendOtpEmail(String to, String otpCode, int expiryMinutes) throws EmailSendException;

    /** Thrown when an email cannot be delivered. */
    class EmailSendException extends RuntimeException {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}