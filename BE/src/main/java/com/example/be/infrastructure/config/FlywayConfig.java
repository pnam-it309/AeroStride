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

    // Chặn Flyway chạy mặc định trước Hibernate
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Không làm gì ở đây để defer quá trình migrate cho đến khi Hibernate tạo xong bảng
        };
    }

    // Chạy Flyway sau khi toàn bộ ứng dụng (bao gồm Hibernate tạo bảng) đã khởi động xong
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void runFlyway(org.springframework.boot.context.event.ApplicationReadyEvent event) {
        org.flywaydb.core.Flyway flyway = event.getApplicationContext().getBean(org.flywaydb.core.Flyway.class);
        flyway.repair();
        flyway.migrate();
    }
}
