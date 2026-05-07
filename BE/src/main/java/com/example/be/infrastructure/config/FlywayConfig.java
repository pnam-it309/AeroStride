package com.example.be.infrastructure.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration to handle migration strategies.
 * This ensures that checksum mismatches are automatically repaired during local development.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Automatically repair checksum mismatches (e.g. when developers edit existing migration scripts)
            flyway.repair();
            // Then proceed with migration
            flyway.migrate();
        };
    }
}
