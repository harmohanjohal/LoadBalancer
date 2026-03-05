package com.mycompany.javafxapplication1;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.flywaydb.core.Flyway;

/**
 * Runs Flyway database migrations against the configured SQLite database
 * and, when MySQL is configured, against the remote MySQL database as well.
 */
public class MigrationService {

    private static final Logger logger = Logger.getLogger(MigrationService.class.getName());

    public void migrate() {
        // Local SQLite migrations
        Flyway flyway = Flyway.configure()
                .dataSource(AppConfig.getDatabaseJdbcUrl(), null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();

        // Remote MySQL migrations (optional)
        if (MySQLConfig.isConfigured()) {
            try {
                MySQLConfig.validateOrThrow();
                Flyway mysqlFlyway = Flyway.configure()
                        .dataSource(MySQLConfig.getJdbcUrl(),
                                    MySQLConfig.getUser(),
                                    MySQLConfig.getPassword())
                        .locations("classpath:db/mysql")
                        .baselineOnMigrate(true)
                        .load();
                mysqlFlyway.migrate();
                logger.info("MySQL migrations completed successfully");
            } catch (Exception e) {
                logger.log(Level.WARNING, "MySQL migration failed – remote features disabled", e);
            }
        }
    }
}
