package com.cloudnest.auth.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Minimal RFC 6238 TOTP implementation (HMAC-SHA1, 6 digits, 30s period).
 * <p>
 * Uses only the JDK — no external dependency — so authenticator apps
 * (Google Authenticator, Microsoft Authenticator, Authy, ...) can be used.
 * Secrets are 160-bit random values encoded with RFC 4648 base32.
 */
public final class TotpUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int SECRET_BYTES = 20;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private TotpUtil() {
        // Utility class
    }

    /** Generates a fresh 160-bit base32 secret (no padding). */
    public static String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** Computes the current TOTP code for the given base32 secret. */
    public static String generateCode(String base32Secret) {
        long counter = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        return generateCode(base32Secret, counter);
    }

    /** Computes a TOTP code for an explicit time counter (used by verify). */
    static String generateCode(String base32Secret, long counter) {
        try {
            byte[] key = base32Decode(base32Secret);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));

            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int code = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }

    /**
     * Verifies a code allowing {@code window} time steps of skew in either
     * direction (the code remains valid for a few seconds around the window).
     */
    public static boolean verify(String base32Secret, String code, int window) {
        if (code == null || code.isBlank() || base32Secret == null || base32Secret.isBlank()) {
            return false;
        }
        String normalized = code.trim();
        long currentCounter = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;

        for (int i = -window; i <= window; i++) {
            if (constantTimeEquals(generateCode(base32Secret, currentCounter + i), normalized)) {
                return true;
            }
        }
        return false;
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigestUtil.constantTimeEquals(a, b);
    }

    // ── Base32 (RFC 4648, uppercase, no padding) ───────────────────────────

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET.charAt((value >>> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1F));
        }
        return sb.toString();
    }

    private static byte[] base32Decode(String input) {
        String cleaned = input.toUpperCase(Locale.ROOT).replace(" ", "").replace("=", "");
        ByteBuffer buffer = ByteBuffer.allocate(cleaned.length() * 5 / 8 + 1);
        int bits = 0;
        int value = 0;
        for (char c : cleaned.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("Invalid base32 character: " + c);
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                buffer.put((byte) ((value >>> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        byte[] out = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, out, 0, out.length);
        return out;
    }

    /**
     * Small helper so constant-time comparison can be shared (the JDK's
     * {@code MessageDigest.isEqual} throws if arrays differ in length).
     */
    static final class MessageDigestUtil {
        static boolean constantTimeEquals(String a, String b) {
            byte[] ba = a.getBytes(StandardCharsets.UTF_8);
            byte[] bb = b.getBytes(StandardCharsets.UTF_8);
            return java.security.MessageDigest.isEqual(ba, bb);
        }
    }

    /** Convenience for tests: decode a base32 secret to inspect it. */
    static String toBase64ForDebug(String base32Secret) {
        return Base64.getEncoder().encodeToString(base32Decode(base32Secret));
    }
}
