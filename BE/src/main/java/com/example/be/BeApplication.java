package com.example.be;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class    BeApplication {

    public static void main(String[] args) {
        // Load environment variables from BE/env/.env.dev
        Dotenv dotenv = Dotenv.configure()
                .directory("env")
                .filename(".env.dev")
                .ignoreIfMissing()
                .load();

        // Populate system properties only if not already set by environment variables
        for (DotenvEntry entry : dotenv.entries()) {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }

        SpringApplication.run(BeApplication.class, args);
    }

}
