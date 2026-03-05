package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AppConfig {

    public static final String ENV_FILE_ENCRYPTION_PASSWORD = "FILE_ENCRYPTION_PASSWORD";
    public static final String ENV_APP_DB_PATH = "APP_DB_PATH";
    private static final String DEFAULT_DB_FILE = "comp20081.db";

    private AppConfig() {
    }

    public static void validateOrThrow() {
        List<String> errors = new ArrayList<>();

        String encryptionPassword = System.getenv(ENV_FILE_ENCRYPTION_PASSWORD);
        if (encryptionPassword == null || encryptionPassword.isBlank()) {
            errors.add("Missing required environment variable: " + ENV_FILE_ENCRYPTION_PASSWORD);
        } else if (encryptionPassword.length() < 12) {
            errors.add(ENV_FILE_ENCRYPTION_PASSWORD + " must be at least 12 characters long.");
        }

        String configuredDbPath = System.getenv(ENV_APP_DB_PATH);
        if (configuredDbPath != null && !configuredDbPath.isBlank()) {
            String normalizedPath = configuredDbPath.startsWith("jdbc:sqlite:")
                    ? configuredDbPath.substring("jdbc:sqlite:".length())
                    : configuredDbPath;

            try {
                File dbFile = new File(normalizedPath).getCanonicalFile();
                File parent = dbFile.getParentFile();
                if (parent != null && parent.exists() && !parent.canWrite()) {
                    errors.add("Database directory is not writable: " + parent.getAbsolutePath());
                }
            } catch (IOException e) {
                errors.add("Invalid database path: " + normalizedPath);
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join("\n", errors));
        }
    }

    public static String getDatabaseJdbcUrl() {
        String configuredDbPath = System.getenv(ENV_APP_DB_PATH);
        if (configuredDbPath != null && !configuredDbPath.isBlank()) {
            if (configuredDbPath.startsWith("jdbc:sqlite:")) {
                String rawPath = configuredDbPath.substring("jdbc:sqlite:".length());
                try {
                    String canonicalPath = new File(rawPath).getCanonicalPath();
                    return "jdbc:sqlite:" + canonicalPath;
                } catch (IOException e) {
                    throw new IllegalStateException("Invalid database path: " + rawPath, e);
                }
            }
            try {
                String canonicalPath = new File(configuredDbPath).getCanonicalPath();
                return "jdbc:sqlite:" + canonicalPath;
            } catch (IOException e) {
                throw new IllegalStateException("Invalid database path: " + configuredDbPath, e);
            }
        }

        String absolutePath = new File(DEFAULT_DB_FILE).getAbsolutePath();
        return "jdbc:sqlite:" + absolutePath;
    }
}
