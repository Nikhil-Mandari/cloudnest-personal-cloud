package com.cloudnest.auth.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Dependency-free RFC 6238 (TOTP) implementation with RFC 4648 base32.
 * <p>
 * Uses HMAC-SHA1, 30-second time steps and 6-digit codes — the defaults
 * understood by Google Authenticator, Microsoft Authenticator and Authy.
 * Verification accepts a configurable window of ±N time steps to tolerate
 * clock drift, and compares codes in constant time.
 */
public final class TotpUtils {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;

    private TotpUtils() {
    }

    /** Generates a fresh 160-bit base32 TOTP secret (no padding). */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Builds the otpauth:// URI an authenticator app scans as a QR code. */
    public static String generateOtpAuthUri(String secret, String accountName, String issuer) {
        return "otpauth://totp/" + percentEncode(issuer) + ":" + percentEncode(accountName)
                + "?secret=" + secret
                + "&issuer=" + percentEncode(issuer)
                + "&algorithm=SHA1&digits=" + CODE_DIGITS + "&period=" + TIME_STEP_SECONDS;
    }

    /**
     * Verifies a 6-digit code against the secret with a default ±1 step window.
     */
    public static boolean verify(String code, String secret) {
        return findMatchingCounter(code, secret, 1).isPresent();
    }

    /**
     * Verifies a code against the secret within {@code window} steps either
     * side of the current time step.
     */
    public static boolean verify(String code, String secret, int window) {
        return findMatchingCounter(code, secret, window).isPresent();
    }

    /**
     * Finds the TOTP time-step counter whose code matches, within
     * {@code window} steps either side of now. Callers use the returned
     * counter for replay protection: a counter that is not greater than the
     * last accepted one must be rejected.
     *
     * @return the matching counter, or empty when the code is invalid
     */
    public static java.util.Optional<Long> findMatchingCounter(String code, String secret, int window) {
        if (code == null || secret == null || !code.matches("\\d{6}")) {
            return java.util.Optional.empty();
        }
        byte[] key;
        try {
            key = base32Decode(secret);
        } catch (IllegalArgumentException e) {
            return java.util.Optional.empty();
        }
        if (key.length == 0) {
            return java.util.Optional.empty();
        }
        long counter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (int offset = -window; offset <= window; offset++) {
            if (constantTimeEquals(code, generateCode(key, counter + offset))) {
                return java.util.Optional.of(counter + offset);
            }
        }
        return java.util.Optional.empty();
    }

    /** SHA-256 hex digest (used to hash backup codes at rest). */
    public static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ── RFC 6238 core ───────────────────────────────────────────────────────

    private static String generateCode(byte[] key, long counter) {
        byte[] data = new byte[8];
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) counter;
            counter >>>= 8;
        }
        byte[] hash = hmacSha1(key, data);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % 1_000_000;
        return String.format("%06d", otp);
    }

    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    // ── RFC 4648 base32 ─────────────────────────────────────────────────────

    private static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder(((data.length + 4) / 5) * 8);
        int buffer = 0;
        int bits = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                out.append(ALPHABET.charAt((buffer >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            out.append(ALPHABET.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return out.toString();
    }

    private static byte[] base32Decode(String input) {
        String value = input.replace("=", "").toUpperCase();
        java.util.List<Byte> bytes = new java.util.ArrayList<>();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < value.length(); i++) {
            int index = ALPHABET.indexOf(value.charAt(i));
            if (index < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + value.charAt(i));
            }
            buffer = (buffer << 5) | index;
            bits += 5;
            if (bits >= 8) {
                bytes.add((byte) ((buffer >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    /** Minimal percent-encoding for the otpauth URI label/issuer. */
    private static String percentEncode(String value) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return sb.toString();
    }
}
