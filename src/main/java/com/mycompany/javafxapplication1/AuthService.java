package com.mycompany.javafxapplication1;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

/**
 * Service layer for user authentication and registration.
 */
public class AuthService {

    private final UserRepository userRepository;

    public AuthService() {
        this(new SQLiteUserRepository());
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String username, String password)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        String storedPasswordHash = userRepository.findPasswordHashByUsername(username);
        if (storedPasswordHash == null) {
            return null;
        }

        if (!PasswordEncryptor.verifyPassword(password, storedPasswordHash)) {
            return null;
        }

        String role = userRepository.findRoleByUsername(username);
        return new User(username, null, role);
    }

    public boolean registerUser(String username, String password, String role) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        return userRepository.createUser(username, password, role);
    }
}
