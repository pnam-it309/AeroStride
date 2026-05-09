package com.example.be;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BeApplication {

    public static void main(String[] args) {
        System.err.println("[AeroStride] Initializing environment...");
        System.out.println("ENVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
        // Load environment variables from BE/env/.env.dev or env/.env.dev
        Dotenv dotenv = null;
        String[] paths = {"env", "BE/env", "."};

        for (String path : paths) {
            try {
                dotenv = Dotenv.configure()
                        .directory(path)
                        .filename(".env.dev")
                        .load();
                System.err.println("[AeroStride] SUCCESS: Loaded .env.dev from " + path + " (" + dotenv.entries().size() + " entries)");
                break;
            } catch (Exception e) {
                System.err.println("[AeroStride] INFO: Could not load from " + path + "/.env.dev");
            }
        }

        if (dotenv == null) {
            System.err.println("[AeroStride] ERROR: Could not find .env.dev in any standard location!");
        } else {
            for (DotenvEntry entry : dotenv.entries()) {
                // Set system property if not exists or if it's a critical app property
                if (System.getProperty(entry.getKey()) == null || entry.getKey().startsWith("SPRING_") || entry.getKey().startsWith("JWT_")) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        SpringApplication.run(BeApplication.class, args);
    }
}
