package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;

/**
 * Repository interface for user persistence operations.
 */
public interface UserRepository {
    List<User> findAllUsers() throws SQLException;

    String findPasswordHashByUsername(String username) throws SQLException;

    String findRoleByUsername(String username) throws SQLException;

    boolean createUser(String username, String password, String role) throws SQLException;

    boolean updateUsername(String currentUsername, String newUsername);

    boolean updatePassword(String username, String newPassword);

    boolean deleteUser(String username);
}
