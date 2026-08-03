package com.cloudnest.file.util;

import com.cloudnest.file.exception.FileStorageException;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for computing SHA-256 checksums of uploaded file content.
 * <p>
 * The checksum is stored in the database at upload time and can be used in the
 * future for duplicate-content detection and integrity verification.
 */
public final class ChecksumUtil {

    private static final int BUFFER_SIZE = 8192;

    private ChecksumUtil() {
        // Utility class — not instantiable
    }

    /**
     * Computes the SHA-256 checksum (lowercase hex) of the given stream.
     * The stream is fully consumed.
     *
     * @param input the input stream to hash
     * @return the SHA-256 checksum as a 64-character lowercase hex string
     * @throws FileStorageException if the stream cannot be read or SHA-256 is unavailable
     */
    public static String sha256Hex(InputStream input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new FileStorageException("Failed to compute SHA-256 checksum", e);
        }
    }

    /**
     * Computes the SHA-256 checksum (lowercase hex) of the given byte array.
     *
     * @param data the bytes to hash
     * @return the SHA-256 checksum as a 64-character lowercase hex string
     * @throws FileStorageException if SHA-256 is unavailable
     */
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("Failed to compute SHA-256 checksum", e);
        }
    }

    /**
     * Converts a byte array into a lowercase hex string.
     *
     * @param bytes the bytes to convert
     * @return the hex representation
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
