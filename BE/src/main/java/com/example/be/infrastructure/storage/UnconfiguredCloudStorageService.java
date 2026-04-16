package com.example.be.infrastructure.storage;

import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.exceptions.StorageProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile("!local")
@ConditionalOnExpression(
        "!T(org.springframework.util.StringUtils).hasText('${cloudinary.cloud_name:}') || " +
        "!T(org.springframework.util.StringUtils).hasText('${cloudinary.api_key:}') || " +
        "!T(org.springframework.util.StringUtils).hasText('${cloudinary.api_secret:}')"
)
@Slf4j
public class UnconfiguredCloudStorageService implements StorageService {

    private static final String CONFIG_ERROR_MESSAGE =
            "Cloud storage is not configured. Please set CLOUDINARY_NAME, CLOUDINARY_KEY, and CLOUDINARY_SECRET in BE/env/.env.dev before uploading images.";

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folderName) {
        log.error("Image upload blocked because Cloudinary configuration is missing. Folder requested: {}", folderName);
        throw new StorageProcessingException(CONFIG_ERROR_MESSAGE);
    }

    @Override
    public void deleteFile(String publicId) {
        log.error("Cloud file deletion blocked because Cloudinary configuration is missing. Public ID: {}", publicId);
        throw new StorageProcessingException(CONFIG_ERROR_MESSAGE);
    }
}

