package com.example.be.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LocalUploadResourceConfig implements WebMvcConfigurer {

    @Value("${app.local-upload-dir}")
    private String localUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadRoot = Paths.get(localUploadDir).toAbsolutePath().normalize();
        File uploadFolder = uploadRoot.toFile();
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }
        String uploadUri = uploadRoot.toUri().toString();
        if (!uploadUri.endsWith("/")) {
            uploadUri += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadUri);
    }
}
