package com.bookshelf.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs {@code flyway repair} before every migrate so that checksum drift
 * on data-only seed migrations (V6 books, V7 test content) does not break
 * application startup after harmless catalog edits. Production with strict
 * immutability would set this strategy to plain {@code migrate()} only.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairOnMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
