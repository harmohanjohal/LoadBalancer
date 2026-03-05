package com.mycompany.javafxapplication1;

import org.flywaydb.core.Flyway;

/**
 * Runs Flyway database migrations against the configured SQLite database.
 */
public class MigrationService {

    public void migrate() {
        Flyway flyway = Flyway.configure()
                .dataSource(AppConfig.getDatabaseJdbcUrl(), null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
    }
}
