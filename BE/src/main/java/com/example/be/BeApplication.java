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

        Dotenv dotenv = null;
        // Danh sách các đường dẫn và tên file tiềm năng để tìm cấu hình môi trường
        String[] locations = {"env", "BE/env", "docker/env", "../docker/env", ".", ".."};
        String[] filenames = {".env.dev", ".env"};

        for (String path : locations) {
            for (String filename : filenames) {
                try {
                    dotenv = Dotenv.configure()
                            .directory(path)
                            .filename(filename)
                            .load();
                    break;
                } catch (Exception e) {
                    // Log silently for non-existent files during search
                }
            }
            if (dotenv != null) break;
        }

        if (dotenv == null) {
            System.err.println("[AeroStride] WARNING: No .env or .env.dev found in standard locations. Relying on System/OS environment variables.");
        } else {
            for (DotenvEntry entry : dotenv.entries()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Các biến quan trọng (SPRING_ và JWT_) luôn được ưu tiên ghi đè vào System Properties
                boolean isCritical = key.startsWith("SPRING_") || key.startsWith("JWT_");
                
                if (System.getProperty(key) == null || isCritical) {
                    System.setProperty(key, value);
                }
            }
        }

        SpringApplication.run(BeApplication.class, args);
    }
}
