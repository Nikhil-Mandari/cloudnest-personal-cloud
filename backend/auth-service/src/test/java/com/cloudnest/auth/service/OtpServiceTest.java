package com.cloudnest.auth.service;

import com.cloudnest.auth.config.AuthProperties;
import com.cloudnest.auth.entity.OtpVerification;
import com.cloudnest.auth.entity.UserCredential;
import com.cloudnest.auth.exception.OtpExpiredException;
import com.cloudnest.auth.exception.OtpInvalidException;
import com.cloudnest.auth.exception.OtpMaxAttemptsException;
import com.cloudnest.auth.exception.OtpResendCooldownException;
import com.cloudnest.auth.repository.OtpVerificationRepository;
import com.cloudnest.auth.util.Hashing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link OtpService} (no Spring context required).
 */
class OtpServiceTest {

    private OtpVerificationRepository repository;
    private EmailService emailService;
    private AuthProperties properties;
    private OtpService service;

    private static final UserCredential USER = UserCredential.builder()
            .id(1L)
            .username("tester")
            .email("tester@example.com")
            .build();

    @BeforeEach
    void setUp() {
        repository = mock(OtpVerificationRepository.class);
        emailService = mock(EmailService.class);
        properties = new AuthProperties();
        properties.getOtp().setLength(6);
        properties.getOtp().setExpiryMinutes(5);
        properties.getOtp().setMaxAttempts(5);
        properties.getOtp().setResendCooldownSeconds(60);
        properties.getOtp().setPepper("test-pepper");
        service = new OtpService(repository, emailService, properties);
    }

    @Test
    @DisplayName("Generates a 6-digit code, stores its hash and emails it")
    void generateAndSend_storesHashedCodeAndEmails() {
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());
        when(emailService.isEnabled()).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OtpService.OtpDispatchResult result =
                service.generateAndSend(USER, OtpVerification.Purpose.REGISTRATION);

        assertNotNull(result.devOtp());
        assertEquals(6, result.devOtp().length());
        assertTrue(result.devOtp().chars().allMatch(Character::isDigit));

        // The persisted record must contain the SHA-256 hash, never the raw code.
        ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
        verify(repository).save(captor.capture());
        OtpVerification captured = captor.getValue();
        assertEquals(Hashing.hmacSha256Hex(result.devOtp(), "test-pepper"), captured.getCodeHash());
        assertEquals(0, captured.getAttempts());

        // The raw code (not the hash) goes to the mailer.
        verify(emailService).sendOtp(anyString(), anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("Rejects a resend during the cooldown window")
    void generateAndSend_respectsCooldown() {
        OtpVerification existing = OtpVerification.builder()
                .id(1L)
                .userId(USER.getId())
                .purpose(OtpVerification.Purpose.LOGIN)
                .codeHash("hash")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(5)
                .requestedAt(LocalDateTime.now().minusSeconds(5))
                .resentAt(LocalDateTime.now().minusSeconds(5))
                .build();
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.of(existing));

        assertThrows(OtpResendCooldownException.class,
                () -> service.generateAndSend(USER, OtpVerification.Purpose.LOGIN));
    }

    @Test
    @DisplayName("Verifies a correct code and consumes the record")
    void verify_acceptsCorrectCode() {
        OtpVerification otp = otp(Hashing.hmacSha256Hex("123456", "test-pepper"));
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.of(otp));

        service.verify(USER, OtpVerification.Purpose.REGISTRATION, "123456");

        assertTrue(Boolean.TRUE.equals(otp.getVerified()));
        assertTrue(Boolean.TRUE.equals(otp.getConsumed()));
    }

    @Test
    @DisplayName("Rejects a wrong code and increments the attempt counter")
    void verify_rejectsWrongCode() {
        OtpVerification otp = otp(Hashing.hmacSha256Hex("123456", "test-pepper"));
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.of(otp));

        assertThrows(OtpInvalidException.class,
                () -> service.verify(USER, OtpVerification.Purpose.REGISTRATION, "999999"));
        assertEquals(1, otp.getAttempts());
    }

    @Test
    @DisplayName("Rejects an expired code")
    void verify_rejectsExpired() {
        OtpVerification otp = otp(Hashing.hmacSha256Hex("123456", "test-pepper"));
        otp.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.of(otp));

        assertThrows(OtpExpiredException.class,
                () -> service.verify(USER, OtpVerification.Purpose.REGISTRATION, "123456"));
    }

    @Test
    @DisplayName("Consumes the record once the attempt budget is exhausted")
    void verify_exhaustsAttempts() {
        OtpVerification otp = otp(Hashing.hmacSha256Hex("123456", "test-pepper"));
        otp.setAttempts(5);
        when(repository.findFirstByUserIdAndPurposeAndConsumedFalseOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.of(otp));

        assertThrows(OtpMaxAttemptsException.class,
                () -> service.verify(USER, OtpVerification.Purpose.REGISTRATION, "000000"));
        assertTrue(Boolean.TRUE.equals(otp.getConsumed()));
    }

    // -- Helpers -----------------------------------------------------------

    private OtpVerification otp(String codeHash) {
        return OtpVerification.builder()
                .id(1L)
                .userId(USER.getId())
                .purpose(OtpVerification.Purpose.REGISTRATION)
                .codeHash(codeHash)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .maxAttempts(5)
                .requestedAt(LocalDateTime.now())
                .resentAt(LocalDateTime.now())
                .build();
    }

}
