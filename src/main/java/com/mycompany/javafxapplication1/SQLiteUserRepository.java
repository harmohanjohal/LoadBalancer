package com.mycompany.javafxapplication1;

import java.sql.SQLException;
import java.util.List;

/**
 * SQLite-backed implementation of {@link UserRepository}.
 */
public class SQLiteUserRepository implements UserRepository {

    private final DB db;

    public SQLiteUserRepository() {
        this.db = new DB();
    }

    @Override
    public List<User> findAllUsers() throws SQLException {
        return db.getUsers();
    }

    @Override
    public String findPasswordHashByUsername(String username) throws SQLException {
        return db.getPasswordForUser(username);
    }

    @Override
    public String findRoleByUsername(String username) throws SQLException {
        return db.getUserRole(username);
    }

    @Override
    public boolean createUser(String username, String password, String role) throws SQLException {
        return db.registerUser(username, password, role);
    }

    @Override
    public boolean updateUsername(String currentUsername, String newUsername) {
        return db.updateUsername(currentUsername, newUsername);
    }

    @Override
    public boolean updatePassword(String username, String newPassword) {
        return db.updatePassword(username, newPassword);
    }

    @Override
    public boolean deleteUser(String username) {
        return db.deleteUser(username);
    }
}
