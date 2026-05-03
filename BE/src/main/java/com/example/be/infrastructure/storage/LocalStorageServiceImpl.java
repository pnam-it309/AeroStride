package com.example.be.infrastructure.storage;

import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.exceptions.StorageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Primary
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalStorageServiceImpl implements StorageService {

    @Value("${app.base_url}")
    private String appBaseUrl;

    @Value("${app.local-upload-dir}")
    private String localUploadDir;

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folderName) {
        if (file == null || file.isEmpty()) {
            throw new StorageProcessingException("Uploaded file is empty");
        }

        String sanitizedFolder = sanitizeFolder(folderName);
        String extension = extractExtension(file.getOriginalFilename());
        String generatedFileName = UUID.randomUUID() + extension;
        Path rootPath = Paths.get(localUploadDir).toAbsolutePath().normalize();
        Path targetDirectory = rootPath.resolve(sanitizedFolder).normalize();
        Path targetFile = targetDirectory.resolve(generatedFileName).normalize();

        if (!targetFile.startsWith(rootPath)) {
            throw new StorageProcessingException("Invalid upload target path");
        }

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            String publicId = sanitizedFolder + "/" + generatedFileName;
            String baseUrl = StringUtils.trimTrailingCharacter(appBaseUrl, '/');
            return FileUploadResult.builder()
                    .fileUrl(baseUrl + "/uploads/" + publicId)
                    .publicId(publicId)
                    .format(extension.isBlank() ? null : extension.substring(1))
                    .build();
        } catch (IOException e) {
            log.error("Failed to store local upload in folder [{}]", sanitizedFolder, e);
            throw new StorageProcessingException("Failed to store file locally", e);
        }
    }

    @Override
    public void deleteFile(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }

        Path rootPath = Paths.get(localUploadDir).toAbsolutePath().normalize();
        Path targetFile = rootPath.resolve(publicId).normalize();

        if (!targetFile.startsWith(rootPath)) {
            throw new StorageProcessingException("Invalid file path");
        }

        try {
            Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            log.error("Failed to delete local file [{}]", publicId, e);
            throw new StorageProcessingException("Failed to delete local file", e);
        }
    }

    private String sanitizeFolder(String folderName) {
        String fallback = "aerostride/general";
        if (!StringUtils.hasText(folderName)) {
            return fallback;
        }

        String normalized = folderName.trim()
                .replace('\\', '/')
                .replaceAll("\\.\\.+", "")
                .replaceAll("/+", "/")
                .replaceAll("^[./]+", "")
                .replaceAll("[^a-zA-Z0-9/_-]", "-");

        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String extractExtension(String originalFileName) {
        if (!StringUtils.hasText(originalFileName) || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.'));
    }
}
