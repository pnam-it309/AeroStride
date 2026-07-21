package com.example.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Standard Integration Test platform using Testcontainers and MockMvc.
 * Everything is pre-configured for sub-classes: Database, MockMvc, ObjectMapper.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

    protected static MySQLContainer<?> mysql;

    static {
        // Configure Testcontainers for Docker Desktop on Windows
        System.setProperty("docker.host", "npipe:////./pipe/docker_cli");
        try {
            mysql = new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("aerostride_test")
                    .withUsername("test")
                    .withPassword("test");
        } catch (Throwable t) {
            System.err.println("WARN: Failed to initialize MySQL testcontainer (Docker might be offline): " + t.getMessage());
        }
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (mysql != null) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    // Global helper for complex JSON mapping if needed
    protected String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
