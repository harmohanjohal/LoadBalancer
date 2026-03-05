package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bidirectional synchronisation between the local SQLite database and the
 * remote MySQL database.
 * <p>
 * Users are synced first (remote is the source of truth), followed by file
 * metadata (newest timestamp wins). Deletions are propagated from local to
 * remote by inspecting the {@code audit_log} table.
 */
public class SyncService {

    private static final Logger logger = Logger.getLogger(SyncService.class.getName());
    private final DB localDb;
    private final MySQLDB remoteDb;

    public SyncService() {
        this.localDb = new DB();
        this.remoteDb = new MySQLDB();
    }

    /** Run a full bidirectional sync.  No-op when MySQL is not configured. */
    public void syncAll() {
        if (!MySQLConfig.isConfigured()) {
            logger.info("MySQL not configured – skipping sync");
            return;
        }

        AuditLogger.log(null, AuditLogger.Action.SYNC_STARTED, "all",
                "Starting database synchronisation");
        try {
            syncUsers();
            syncFileMetadata();
            AuditLogger.log(null, AuditLogger.Action.SYNC_COMPLETED, "all",
                    "Database synchronisation completed");
        } catch (Exception e) {
            AuditLogger.logFailure(null, AuditLogger.Action.SYNC_FAILED, "all", e.getMessage());
            logger.log(Level.SEVERE, "Database synchronisation failed", e);
        }
    }

    // ── User sync ───────────────────────────────────────────────────

    private void syncUsers() throws SQLException {
        Map<String, SyncRecord> localUsers  = fetchLocalUsers();
        Map<String, SyncRecord> remoteUsers = fetchRemoteUsers();

        Set<String> allUsernames = new HashSet<>();
        allUsernames.addAll(localUsers.keySet());
        allUsernames.addAll(remoteUsers.keySet());

        for (String username : allUsernames) {
            SyncRecord local  = localUsers.get(username);
            SyncRecord remote = remoteUsers.get(username);

            if (local != null && remote == null) {
                pushUserToRemote(local);
            } else if (local == null && remote != null) {
                pullUserToLocal(remote);
            } else if (local != null) {
                ConflictResolver.Resolution res =
                        ConflictResolver.resolveUserConflict(username, local.updatedAt, remote.updatedAt);
                if (res == ConflictResolver.Resolution.USE_REMOTE) {
                    pullUserToLocal(remote);
                } else if (res == ConflictResolver.Resolution.USE_LOCAL) {
                    pushUserToRemote(local);
                }
            }
        }

        propagateUserDeletions();
        logger.info("User sync complete");
    }

    // ── File-metadata sync ──────────────────────────────────────────

    private void syncFileMetadata() throws SQLException {
        Map<String, SyncFileRecord> localFiles  = fetchLocalFiles();
        Map<String, SyncFileRecord> remoteFiles = fetchRemoteFiles();

        Set<String> allFileNames = new HashSet<>();
        allFileNames.addAll(localFiles.keySet());
        allFileNames.addAll(remoteFiles.keySet());

        for (String fileName : allFileNames) {
            SyncFileRecord local  = localFiles.get(fileName);
            SyncFileRecord remote = remoteFiles.get(fileName);

            if (local != null && remote == null) {
                pushFileToRemote(local);
            } else if (local == null && remote != null) {
                pullFileToLocal(remote);
            } else if (local != null) {
                ConflictResolver.Resolution res =
                        ConflictResolver.resolveFileConflict(fileName, local.updatedAt, remote.updatedAt);
                if (res == ConflictResolver.Resolution.USE_REMOTE) {
                    pullFileToLocal(remote);
                } else if (res == ConflictResolver.Resolution.USE_LOCAL) {
                    pushFileToRemote(local);
                }
            }
        }

        propagateFileDeletions();
        logger.info("File-metadata sync complete");
    }

    // ── Fetch helpers ───────────────────────────────────────────────

    private Map<String, SyncRecord> fetchLocalUsers() throws SQLException {
        Map<String, SyncRecord> map = new LinkedHashMap<>();
        String sql = "SELECT username, password, role, updated_at FROM users";
        try (Connection conn = localDb.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SyncRecord r = new SyncRecord(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("updated_at"));
                map.put(r.username, r);
            }
        }
        return map;
    }

    private Map<String, SyncRecord> fetchRemoteUsers() throws SQLException {
        Map<String, SyncRecord> map = new LinkedHashMap<>();
        String sql = "SELECT username, password, role, updated_at FROM users";
        try (Connection conn = remoteDb.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String ts = rs.getString("updated_at");
                SyncRecord r = new SyncRecord(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        ts != null ? ts : "");
                map.put(r.username, r);
            }
        }
        return map;
    }

    private Map<String, SyncFileRecord> fetchLocalFiles() throws SQLException {
        Map<String, SyncFileRecord> map = new LinkedHashMap<>();
        String sql = "SELECT file_name, uploaded_by, upload_date, updated_at FROM files";
        try (Connection conn = localDb.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SyncFileRecord r = new SyncFileRecord(
                        rs.getString("file_name"),
                        rs.getString("uploaded_by"),
                        rs.getString("upload_date"),
                        rs.getString("updated_at"));
                map.put(r.fileName, r);
            }
        }
        return map;
    }

    private Map<String, SyncFileRecord> fetchRemoteFiles() throws SQLException {
        Map<String, SyncFileRecord> map = new LinkedHashMap<>();
        String sql = "SELECT file_name, uploaded_by, upload_date, updated_at FROM files";
        try (Connection conn = remoteDb.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String ts = rs.getString("updated_at");
                SyncFileRecord r = new SyncFileRecord(
                        rs.getString("file_name"),
                        rs.getString("uploaded_by"),
                        rs.getString("upload_date"),
                        ts != null ? ts : "");
                map.put(r.fileName, r);
            }
        }
        return map;
    }

    // ── Push / pull ─────────────────────────────────────────────────

    private void pushUserToRemote(SyncRecord record) throws SQLException {
        remoteDb.upsertUserRaw(record.username, record.passwordHash, record.role);
        logger.info("Pushed user to remote: " + record.username);
    }

    private void pullUserToLocal(SyncRecord record) throws SQLException {
        String sql = "INSERT INTO users (username, password, role, updated_at) VALUES (?, ?, ?, datetime('now')) "
                   + "ON CONFLICT(username) DO UPDATE SET password = excluded.password, "
                   + "role = excluded.role, updated_at = datetime('now')";
        try (Connection conn = localDb.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.username);
            stmt.setString(2, record.passwordHash);
            stmt.setString(3, record.role);
            stmt.executeUpdate();
        }
        logger.info("Pulled user from remote: " + record.username);
    }

    private void pushFileToRemote(SyncFileRecord record) throws SQLException {
        remoteDb.upsertFileRecord(record.fileName, record.uploadedBy, record.uploadDate);
        logger.info("Pushed file metadata to remote: " + record.fileName);
    }

    private void pullFileToLocal(SyncFileRecord record) throws SQLException {
        String sql = "INSERT INTO files (file_name, uploaded_by, upload_date, updated_at) "
                   + "VALUES (?, ?, ?, datetime('now')) "
                   + "ON CONFLICT(file_name) DO UPDATE SET uploaded_by = excluded.uploaded_by, "
                   + "upload_date = excluded.upload_date, updated_at = datetime('now')";
        try (Connection conn = localDb.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, record.fileName);
            stmt.setString(2, record.uploadedBy);
            stmt.setString(3, record.uploadDate);
            stmt.executeUpdate();
        }
        logger.info("Pulled file metadata from remote: " + record.fileName);
    }

    // ── Deletion propagation (local → remote via audit_log) ─────────

    private void propagateUserDeletions() throws SQLException {
        List<String> deleted = deletedTargets("USER_DELETED");
        for (String username : deleted) {
            remoteDb.deleteUser(username);
            logger.info("Propagated user deletion to remote: " + username);
        }
    }

    private void propagateFileDeletions() throws SQLException {
        List<String> deleted = deletedTargets("FILE_DELETED");
        for (String fileName : deleted) {
            try {
                remoteDb.deleteFileRecord(fileName);
                logger.info("Propagated file deletion to remote: " + fileName);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to propagate file deletion: " + fileName, e);
            }
        }
    }

    private List<String> deletedTargets(String actionName) throws SQLException {
        List<String> targets = new ArrayList<>();
        String sql = "SELECT DISTINCT target FROM audit_log "
                   + "WHERE action = ? AND status = 'SUCCESS'";
        try (Connection conn = localDb.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, actionName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    targets.add(rs.getString("target"));
                }
            }
        }
        return targets;
    }

    // ── Inner record types ──────────────────────────────────────────

    private static class SyncRecord {
        final String username;
        final String passwordHash;
        final String role;
        final String updatedAt;

        SyncRecord(String username, String passwordHash, String role, String updatedAt) {
            this.username     = username;
            this.passwordHash = passwordHash;
            this.role         = role;
            this.updatedAt    = updatedAt != null ? updatedAt : "";
        }
    }

    private static class SyncFileRecord {
        final String fileName;
        final String uploadedBy;
        final String uploadDate;
        final String updatedAt;

        SyncFileRecord(String fileName, String uploadedBy, String uploadDate, String updatedAt) {
            this.fileName   = fileName;
            this.uploadedBy = uploadedBy;
            this.uploadDate = uploadDate;
            this.updatedAt  = updatedAt != null ? updatedAt : "";
        }
    }
}
