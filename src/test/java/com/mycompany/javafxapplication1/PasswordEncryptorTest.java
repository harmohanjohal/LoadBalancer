package com.mycompany.javafxapplication1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordEncryptorTest {

    @Test
    void hashAndVerifyPasswordShouldSucceed() throws Exception {
        String salt = PasswordEncryptor.generateSalt();
        String hash = PasswordEncryptor.hashPassword("P@ssw0rd!", salt);
        String stored = salt + ":" + hash;

        assertTrue(PasswordEncryptor.verifyPassword("P@ssw0rd!", stored));
        assertFalse(PasswordEncryptor.verifyPassword("wrong-password", stored));
    }

    @Test
    void verifyPasswordShouldThrowOnInvalidStoredFormat() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncryptor.verifyPassword("abc", "invalid-format"));
    }
}
