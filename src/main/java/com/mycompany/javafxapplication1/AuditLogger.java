package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Centralised audit logger that records all user actions and system events
 * to both a rotating log file ({@code logs/audit.log}) and the local
 * {@code audit_log} database table.
 */
public class AuditLogger {

    private static final Logger logger = Logger.getLogger("AUDIT");
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Categories of auditable actions. */
    public enum Action {
        LOGIN_SUCCESS, LOGIN_FAILURE, LOGIN_LOCKED,
        LOGOUT, SESSION_EXPIRED,
        USER_REGISTERED, USER_USERNAME_UPDATED, USER_PASSWORD_UPDATED, USER_DELETED,
        FILE_UPLOADED, FILE_DOWNLOADED, FILE_EDITED, FILE_DELETED,
        SYNC_STARTED, SYNC_COMPLETED, SYNC_FAILED, SYNC_CONFLICT,
        APP_STARTED, APP_STOPPED
    }

    static {
        try {
            new File("logs").mkdirs();
            // 5 MB per file, keep 3 rotated files, append mode
            FileHandler fh = new FileHandler("logs/audit.log", 5_000_000, 3, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
            logger.setUseParentHandlers(true);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not initialise audit log file handler", e);
        }
    }

    private AuditLogger() { }

    /** Log a successful action. */
    public static void log(String username, Action action, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        logger.info(formatMessage(timestamp, username, action, target, details, "SUCCESS"));
        persistToDb(timestamp, username, action.name(), target, details, "SUCCESS");
    }

    /** Log a failed action. */
    public static void logFailure(String username, Action action, String target, String details) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        logger.warning(formatMessage(timestamp, username, action, target, details, "FAILURE"));
        persistToDb(timestamp, username, action.name(), target, details, "FAILURE");
    }

    private static String formatMessage(String timestamp, String username, Action action,
                                         String target, String details, String status) {
        return String.format("[%s] user=%s action=%s target=%s status=%s details=%s",
                timestamp,
                username != null ? username : "SYSTEM",
                action,
                target != null ? target : "-",
                status,
                details != null ? details : "-");
    }

    private static void persistToDb(String timestamp, String username, String action,
                                     String target, String details, String status) {
        String sql = "INSERT INTO audit_log (timestamp, username, action, target, details, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(AppConfig.getDatabaseJdbcUrl());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, timestamp);
            stmt.setString(2, username);
            stmt.setString(3, action);
            stmt.setString(4, target);
            stmt.setString(5, details);
            stmt.setString(6, status);
            stmt.executeUpdate();
        } catch (Exception e) {
            // Silently fall back – table may not exist during early startup
            logger.log(Level.FINE, "Could not persist audit log to database", e);
        }
    }
}
