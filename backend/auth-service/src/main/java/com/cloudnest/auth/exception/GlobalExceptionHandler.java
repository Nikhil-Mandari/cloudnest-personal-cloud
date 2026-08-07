package com.cloudnest.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Auth Service.
 * <p>
 * Catches exceptions thrown from controllers and returns a consistent
 * JSON error response matching the CloudNest API contract.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation failures from {@code @Valid} annotated request bodies.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message("Validation failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles duplicate resource conflicts (e.g. username/email already taken).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles OTP verification failures (invalid, expired, max attempts,
     * resend cooldown).
     */
    @ExceptionHandler(OtpException.class)
    public ResponseEntity<ErrorResponse> handleOtpException(
            OtpException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles sign-in on a temporarily locked account (423 Locked).
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            AccountLockedException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.LOCKED, request);
    }

    /**
     * Handles sign-in attempts on an unverified account (403).
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    /**
     * Handles sign-in attempts on a disabled account (403).
     */
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            AccountDisabledException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    /**
     * Handles non-admin callers on admin-only endpoints (403).
     */
    @ExceptionHandler(AdminAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAdminAccessDenied(
            AdminAccessDeniedException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.FORBIDDEN, request);
    }

    /**
     * Handles invalid / revoked / expired refresh tokens (401).
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.UNAUTHORIZED, request);
    }

    /**
     * Handles unknown sessions (404).
     */
    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(
            SessionNotFoundException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles unknown trusted devices (404).
     */
    @ExceptionHandler(TrustedDeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrustedDeviceNotFound(
            TrustedDeviceNotFoundException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles invalid credentials (wrong password during login).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message("Invalid username/email or password")
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles user not found scenarios.
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles illegal argument / bad request scenarios.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        return buildError(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    // -- Private helpers -----------------------------------------------------

    private ResponseEntity<ErrorResponse> buildError(String message, HttpStatus status, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(message)
                .status(status.value())
                .error(status.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Catch-all for unexpected runtime exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaught(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception [{}] {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message("An unexpected error occurred")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .path(request.getRequestURI())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
