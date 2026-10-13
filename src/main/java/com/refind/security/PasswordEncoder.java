package com.refind.security;

import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Provides secure password hashing and verification using BCrypt with salt rounds.
 * Supports backward-compatible verification and automatic migration from legacy SHA-256 hashes.
 */
public final class PasswordEncoder {

    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordEncoder() {}

    /**
     * Hashes a plaintext password using BCrypt with salt rounds.
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be blank.");
        }
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * Verifies a raw plaintext password against a stored password hash.
     * Supports both BCrypt hashes ($2a$, $2b$, $2y$) and legacy hashes.
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        // Standard BCrypt hashes
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(rawPassword, storedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // Legacy 64-character SHA-256 hex hash
        if (storedHash.length() == 64) {
            String sha256 = legacySha256(rawPassword);
            return MessageDigest.isEqual(
                    sha256.getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8)
            );
        }

        // Backward compatibility for demo seeded placeholder ("hash" or "sha256hash")
        return storedHash.equals(rawPassword) || storedHash.equals("sha256hash");
    }

    /**
     * Returns true if the stored hash is not in standard BCrypt format and should be upgraded.
     */
    public static boolean isLegacyHash(String storedHash) {
        return storedHash != null && !storedHash.startsWith("$2a$") && !storedHash.startsWith("$2b$") && !storedHash.startsWith("$2y$");
    }

    private static String legacySha256(String s) {
        try {
            byte[] b = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte v : b) {
                sb.append(String.format("%02x", v));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
