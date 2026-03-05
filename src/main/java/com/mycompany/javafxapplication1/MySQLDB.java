package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Low-level data-access object for the remote MySQL database.
 * <p>
 * Mirrors the SQLite {@link DB} class but uses MySQL-compatible SQL.
 */
public class MySQLDB {

    private static final Logger logger = Logger.getLogger(MySQLDB.class.getName());

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                MySQLConfig.getJdbcUrl(),
                MySQLConfig.getUser(),
                MySQLConfig.getPassword());
    }

    // ── User operations ─────────────────────────────────────────────

    public boolean registerUser(String username, String password, String role) throws SQLException {
        try {
            String salt = PasswordEncryptor.generateSalt();
            String hashedPassword = PasswordEncryptor.hashPassword(password, salt);
            String storedPassword = salt + ":" + hashedPassword;
            return upsertUserRaw(username, storedPassword, role);
        } catch (Exception e) {
            throw new SQLException("Error registering user in MySQL: " + e.getMessage(), e);
        }
    }

    /** Insert or update a user using an already-hashed password (used by sync). */
    public boolean upsertUserRaw(String username, String passwordHash, String role) throws SQLException {
        String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?) "
                     + "ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role), "
                     + "updated_at = NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setString(3, role);
            return stmt.executeUpdate() > 0;
        }
    }

    public String getPasswordForUser(String username) throws SQLException {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("password") : null;
            }
        }
    }

    public String getUserRole(String username) throws SQLException {
        String query = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("role") : "user";
            }
        }
    }

    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete user from MySQL", e);
            return false;
        }
    }

    public boolean updateUsername(String currentUsername, String newUsername) {
        String query = "UPDATE users SET username = ?, updated_at = NOW() WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, newUsername);
            stmt.setString(2, currentUsername);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update username in MySQL", e);
            return false;
        }
    }

    public boolean updatePassword(String username, String newPassword) {
        try {
            String salt = PasswordEncryptor.generateSalt();
            String hashedPassword = PasswordEncryptor.hashPassword(newPassword, salt);
            String storedPassword = salt + ":" + hashedPassword;
            return updatePasswordRaw(username, storedPassword);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update password in MySQL", e);
            return false;
        }
    }

    public boolean updatePasswordRaw(String username, String passwordHash) {
        String query = "UPDATE users SET password = ?, updated_at = NOW() WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, passwordHash);
            stmt.setString(2, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update password hash in MySQL", e);
            return false;
        }
    }

    public List<User> getUsers() {
        List<User> userList = new ArrayList<>();
        String query = "SELECT username, role FROM users";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                userList.add(new User(rs.getString("username"), null, rs.getString("role")));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load users from MySQL", e);
        }
        return userList;
    }

    // ── File operations ─────────────────────────────────────────────

    public void insertFileRecord(String fileName, String uploader) throws SQLException {
        String query = "INSERT INTO files (file_name, uploaded_by, upload_date) VALUES (?, ?, NOW()) "
                     + "ON DUPLICATE KEY UPDATE uploaded_by = VALUES(uploaded_by), updated_at = NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileName);
            stmt.setString(2, uploader);
            stmt.executeUpdate();
        }
    }

    public void upsertFileRecord(String fileName, String uploadedBy, String uploadDate) throws SQLException {
        String query = "INSERT INTO files (file_name, uploaded_by, upload_date) VALUES (?, ?, ?) "
                     + "ON DUPLICATE KEY UPDATE uploaded_by = VALUES(uploaded_by), "
                     + "upload_date = VALUES(upload_date), updated_at = NOW()";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileName);
            stmt.setString(2, uploadedBy);
            stmt.setString(3, uploadDate);
            stmt.executeUpdate();
        }
    }

    public void deleteFileRecord(String fileName) throws SQLException {
        String query = "DELETE FROM files WHERE file_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileName);
            stmt.executeUpdate();
        }
    }

    public void insertChunkRecord(String fileName, String chunkName, String parentFile) throws SQLException {
        String query = "INSERT INTO file_chunks (file_name, chunk_name, parent_file) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, fileName);
            stmt.setString(2, chunkName);
            stmt.setString(3, parentFile);
            stmt.executeUpdate();
        }
    }

    public List<String> getChunksForParent(String parentFile) throws SQLException {
        List<String> chunkNames = new ArrayList<>();
        String query = "SELECT chunk_name FROM file_chunks WHERE parent_file = ? ORDER BY id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parentFile);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chunkNames.add(rs.getString("chunk_name"));
                }
            }
        }
        return chunkNames;
    }

    public void deleteChunksForParent(String parentFileName) {
        String query = "DELETE FROM file_chunks WHERE parent_file = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parentFileName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete chunks from MySQL", e);
        }
    }

    public List<FileRecord> fetchUploadedFiles() throws SQLException {
        List<FileRecord> fileRows = new ArrayList<>();
        String query = "SELECT file_name, uploaded_by, upload_date FROM files ORDER BY upload_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                fileRows.add(new FileRecord(
                        rs.getString("file_name"),
                        rs.getString("uploaded_by"),
                        rs.getString("upload_date")));
            }
        }
        return fileRows;
    }
}
