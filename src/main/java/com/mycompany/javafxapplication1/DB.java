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
 * Low-level data-access object for SQLite.
 * <p>
 * Prefer using the repository interfaces ({@link UserRepository}, {@link FileRepository})
 * and the corresponding service classes rather than calling this class directly from
 * controllers.
 */
public class DB {

    private static final Logger logger = Logger.getLogger(DB.class.getName());

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    DB() {
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(AppConfig.getDatabaseJdbcUrl());
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    public boolean registerUser(String username, String password, String role) throws SQLException {
        try {
            String salt = PasswordEncryptor.generateSalt();
            String hashedPassword = PasswordEncryptor.hashPassword(password, salt);
            String storedPassword = salt + ":" + hashedPassword;

            String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, storedPassword);
                stmt.setString(3, role);
                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            throw new SQLException("Error while registering user: " + e.getMessage(), e);
        }
    }

    public String getPasswordForUser(String username) throws SQLException {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
            }
        }
        return null;
    }

    public String getUserRole(String username) throws SQLException {
        String role = "user";
        String query = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    role = rs.getString("role");
                }
            }
        }
        return role;
    }

    public boolean deleteUser(String username) {
        String query = "DELETE FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete user", e);
            return false;
        }
    }

    public boolean updateUsername(String currentUsername, String newUsername) {
        String query = "UPDATE users SET username = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newUsername);
            pstmt.setString(2, currentUsername);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update username", e);
            return false;
        }
    }

    public boolean updatePassword(String username, String newPassword) {
        String query = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            String salt = PasswordEncryptor.generateSalt();
            String hashedPassword = PasswordEncryptor.hashPassword(newPassword, salt);
            String storedPassword = salt + ":" + hashedPassword;

            pstmt.setString(1, storedPassword);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update password", e);
            return false;
        }
    }

    public List<User> getUsers() {
        List<User> userList = new ArrayList<>();
        String query = "SELECT username, role FROM users";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                userList.add(new User(rs.getString("username"), null, rs.getString("role")));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load users", e);
        }
        return userList;
    }

    public void insertFileRecord(String fileName, String uploader) throws SQLException {
        String query = "INSERT OR REPLACE INTO files (file_name, uploaded_by, upload_date) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, uploader);
            pstmt.setString(3, LocalDateTime.now().toString());
            pstmt.executeUpdate();
        }
    }

    public void deleteFileRecord(String fileName) throws SQLException {
        String query = "DELETE FROM files WHERE file_name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fileName);
            pstmt.executeUpdate();
        }
    }

    public void insertChunkRecord(String fileName, String chunkName, String parentFile) throws SQLException {
        String query = "INSERT INTO file_chunks (file_name, chunk_name, parent_file, chunk_data) VALUES (?, ?, ?, NULL)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, fileName);
            pstmt.setString(2, chunkName);
            pstmt.setString(3, parentFile);
            pstmt.executeUpdate();
        }
    }

    public List<String> getChunksForParent(String parentFile) throws SQLException {
        List<String> chunkNames = new ArrayList<>();
        String query = "SELECT chunk_name FROM file_chunks WHERE parent_file = ? ORDER BY id";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, parentFile);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    chunkNames.add(rs.getString("chunk_name"));
                }
            }
        }
        return chunkNames;
    }

    public void deleteChunksForParent(String parentFileName) {
        String query = "DELETE FROM file_chunks WHERE parent_file = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, parentFileName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to delete chunks for parent file", e);
        }
    }

    public List<FileRecord> fetchUploadedFiles() throws SQLException {
        List<FileRecord> fileRows = new ArrayList<>();
        String query = "SELECT file_name, uploaded_by, upload_date FROM files ORDER BY upload_date DESC";

        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String fileName = rs.getString("file_name");
                String uploadedBy = rs.getString("uploaded_by");
                String uploadDate = rs.getString("upload_date");
                fileRows.add(new FileRecord(fileName, uploadedBy, uploadDate));
            }
        } catch (Exception e) {
            throw new SQLException("Error fetching uploaded files: " + e.getMessage(), e);
        }

        return fileRows;
    }

}
