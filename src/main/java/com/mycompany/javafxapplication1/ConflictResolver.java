package com.mycompany.javafxapplication1;

import java.util.logging.Logger;

/**
 * Determines which version of a record should win when the local SQLite
 * database and the remote MySQL database both hold a changed copy.
 *
 * <h3>Default strategies</h3>
 * <ul>
 *   <li><b>Users</b> – remote (MySQL) is the source of truth for
 *       centralised user management.</li>
 *   <li><b>Files</b> – newest {@code updated_at} timestamp wins;
 *       ties favour the local copy.</li>
 * </ul>
 */
public class ConflictResolver {

    private static final Logger logger = Logger.getLogger(ConflictResolver.class.getName());

    public enum Resolution {
        USE_LOCAL, USE_REMOTE, SKIP
    }

    private ConflictResolver() { }

    /**
     * Resolves a user-record conflict.
     * Policy: remote wins (centralised user management).
     */
    public static Resolution resolveUserConflict(String username,
                                                  String localUpdatedAt,
                                                  String remoteUpdatedAt) {
        AuditLogger.log(null, AuditLogger.Action.SYNC_CONFLICT, username,
                "User conflict – local=" + localUpdatedAt
                        + " remote=" + remoteUpdatedAt + " → REMOTE_WINS");
        return Resolution.USE_REMOTE;
    }

    /**
     * Resolves a file-metadata conflict.
     * Policy: newest timestamp wins; ties favour local.
     */
    public static Resolution resolveFileConflict(String fileName,
                                                  String localUpdatedAt,
                                                  String remoteUpdatedAt) {
        Resolution resolution;
        if (localUpdatedAt != null
                && (remoteUpdatedAt == null || localUpdatedAt.compareTo(remoteUpdatedAt) >= 0)) {
            resolution = Resolution.USE_LOCAL;
        } else {
            resolution = Resolution.USE_REMOTE;
        }

        AuditLogger.log(null, AuditLogger.Action.SYNC_CONFLICT, fileName,
                "File conflict – local=" + localUpdatedAt
                        + " remote=" + remoteUpdatedAt + " → " + resolution);
        return resolution;
    }
}
