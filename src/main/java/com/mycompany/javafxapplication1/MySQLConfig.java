package com.mycompany.javafxapplication1;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the optional remote MySQL database.
 * <p>
 * Connection details are read from environment variables:
 * <ul>
 *   <li>{@code MYSQL_HOST} – hostname or IP (required when MySQL is used)</li>
 *   <li>{@code MYSQL_PORT} – port number (default {@code 3306})</li>
 *   <li>{@code MYSQL_USER} – database user (required)</li>
 *   <li>{@code MYSQL_PASSWORD} – database password (required)</li>
 *   <li>{@code MYSQL_DATABASE} – schema name (required)</li>
 * </ul>
 */
public final class MySQLConfig {

    public static final String ENV_MYSQL_HOST     = "MYSQL_HOST";
    public static final String ENV_MYSQL_PORT     = "MYSQL_PORT";
    public static final String ENV_MYSQL_USER     = "MYSQL_USER";
    public static final String ENV_MYSQL_PASSWORD = "MYSQL_PASSWORD";
    public static final String ENV_MYSQL_DATABASE = "MYSQL_DATABASE";

    private MySQLConfig() { }

    /** Returns {@code true} when at least the host variable is set. */
    public static boolean isConfigured() {
        String host = System.getenv(ENV_MYSQL_HOST);
        return host != null && !host.isBlank();
    }

    /**
     * Validates that all required MySQL variables are present.
     *
     * @throws IllegalStateException if any required variable is missing.
     */
    public static void validateOrThrow() {
        if (!isConfigured()) return;

        List<String> errors = new ArrayList<>();
        if (getEnv(ENV_MYSQL_HOST) == null)     errors.add("Missing " + ENV_MYSQL_HOST);
        if (getEnv(ENV_MYSQL_USER) == null)     errors.add("Missing " + ENV_MYSQL_USER);
        if (getEnv(ENV_MYSQL_PASSWORD) == null) errors.add("Missing " + ENV_MYSQL_PASSWORD);
        if (getEnv(ENV_MYSQL_DATABASE) == null) errors.add("Missing " + ENV_MYSQL_DATABASE);

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "MySQL configuration errors:\n" + String.join("\n", errors));
        }
    }

    /** Builds the JDBC URL for the remote MySQL instance. */
    public static String getJdbcUrl() {
        String host = System.getenv(ENV_MYSQL_HOST);
        String port = System.getenv(ENV_MYSQL_PORT);
        if (port == null || port.isBlank()) port = "3306";
        String database = System.getenv(ENV_MYSQL_DATABASE);
        return "jdbc:mysql://" + host + ":" + port + "/" + database
             + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
    }

    public static String getUser() {
        return System.getenv(ENV_MYSQL_USER);
    }

    public static String getPassword() {
        return System.getenv(ENV_MYSQL_PASSWORD);
    }

    private static String getEnv(String key) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : null;
    }
}
