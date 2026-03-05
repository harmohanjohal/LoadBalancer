package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;

/**
 * Service layer for user CRUD operations, delegating persistence
 * to a {@link UserRepository} implementation.
 */
public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this(new SQLiteUserRepository());
    }

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() throws SQLException {
        return userRepository.findAllUsers();
    }

    public boolean updateUsername(String currentUsername, String newUsername, User actor) {
        assertCanModify(currentUsername, actor);
        return userRepository.updateUsername(currentUsername, newUsername);
    }

    public boolean updatePassword(String username, String newPassword, User actor) {
        assertCanModify(username, actor);
        return userRepository.updatePassword(username, newPassword);
    }

    public boolean deleteUser(String username, User actor) {
        assertCanModify(username, actor);
        return userRepository.deleteUser(username);
    }

    private void assertCanModify(String targetUsername, User actor) {
        if (actor == null) {
            throw new SecurityException("You must be logged in to perform this action.");
        }
        boolean isAdmin = "admin".equalsIgnoreCase(actor.getRole());
        boolean isSelf = actor.getUsername().equals(targetUsername);
        if (!isAdmin && !isSelf) {
            throw new SecurityException("You are not authorized to modify this user.");
        }
    }
}
