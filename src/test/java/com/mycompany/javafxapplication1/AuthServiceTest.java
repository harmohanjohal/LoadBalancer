package com.mycompany.javafxapplication1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private InMemoryUserRepository repository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        authService = new AuthService(repository);
    }

    @Test
    void authenticateShouldReturnUserOnValidCredentials() throws Exception {
        authService.registerUser("alice", "Secret123!", "admin");

        User authenticated = authService.authenticate("alice", "Secret123!");

        assertNotNull(authenticated);
        assertEquals("alice", authenticated.getUsername());
        assertEquals("admin", authenticated.getRole());
    }

    @Test
    void authenticateShouldReturnNullOnInvalidCredentials() throws Exception {
        authService.registerUser("bob", "Secret123!", "user");

        User authenticated = authService.authenticate("bob", "wrong");

        assertNull(authenticated);
    }

    static class InMemoryUserRepository implements UserRepository {

        private final List<User> users = new ArrayList<>();
        private final Map<String, String> passwordHashes = new HashMap<>();

        @Override
        public List<User> findAllUsers() {
            return users;
        }

        @Override
        public String findPasswordHashByUsername(String username) {
            return passwordHashes.get(username);
        }

        @Override
        public String findRoleByUsername(String username) {
            return users.stream().filter(user -> user.getUsername().equals(username)).map(User::getRole).findFirst().orElse("user");
        }

        @Override
        public boolean createUser(String username, String password, String role) throws SQLException {
            try {
                String salt = PasswordEncryptor.generateSalt();
                String hash = PasswordEncryptor.hashPassword(password, salt);
                String storedHash = salt + ":" + hash;
                passwordHashes.put(username, storedHash);
                users.add(new User(username, null, role));
                return true;
            } catch (Exception e) {
                throw new SQLException("Hashing failed in test", e);
            }
        }

        @Override
        public boolean updateUsername(String currentUsername, String newUsername) {
            return true;
        }

        @Override
        public boolean updatePassword(String username, String newPassword) {
            return true;
        }

        @Override
        public boolean deleteUser(String username) {
            return true;
        }
    }
}
