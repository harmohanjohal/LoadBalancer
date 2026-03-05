package com.mycompany.javafxapplication1;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utility class for password hashing and verification using PBKDF2.
 */
public class PasswordEncryptor {

    private static final int PBKDF2_ITERATIONS = 600_000;

    private PasswordEncryptor() {
        // Prevent instantiation of utility class
    }

    public static boolean verifyPassword(String enteredPassword, String storedHash) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Split the stored hash into its components
        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Stored hash is not in the correct format.");
        }

        String salt = parts[0];
        String hash = parts[1];

        // Hash the entered password using the same salt
        String hashedEnteredPassword = hashPassword(enteredPassword, salt);

        // Constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                hashedEnteredPassword.getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, 256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hashBytes = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hashBytes);
        } finally {
            spec.clearPassword();
        }
    }

    public static String generateSalt() {
        // Generate a random salt
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}

