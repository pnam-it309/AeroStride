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
        // Load environment variables from BE/env/.env.dev
        Dotenv dotenv = Dotenv.configure()
                .directory("env")
                .filename(".env.dev")
                .ignoreIfMissing()
                .load();

        // Populate system properties so Spring's ${...} can find them
        for (DotenvEntry entry : dotenv.entries()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        SpringApplication.run(BeApplication.class, args);
    }

}
