package com.example.be.infrastructure.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.exceptions.StorageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Profile("!local")
@ConditionalOnExpression(
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.cloud_name:}') && " +
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.api_key:}') && " +
        "T(org.springframework.util.StringUtils).hasText('${cloudinary.api_secret:}')"
)
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageServiceImpl implements StorageService {

    private final Cloudinary cloudinary;

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folderName) {
        try {
            log.info("Starting file upload to Cloudinary in folder: {}", folderName);

            // Using Cloudinary SDK to upload the file
            // asMap("folder", folderName) allows us to organize files into folders in the Cloudinary Media Library
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto" // Automatically detected resource type (image, video, raw)
            ));

            FileUploadResult result = FileUploadResult.builder()
                    .fileUrl((String) uploadResult.get("secure_url")) // Returns HTTPS URL
                    .publicId((String) uploadResult.get("public_id")) // Unique ID for later management (like delete)
                    .format((String) uploadResult.get("format"))
                    .build();

            log.info("File uploaded successfully. Public ID: {}", result.getPublicId());
            return result;

        } catch (IOException e) {
            log.error("Error occurred while reading file bytes for upload: {}", e.getMessage());
            throw new StorageProcessingException("Could not read file data for upload", e);
        } catch (Exception e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new StorageProcessingException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String publicId) {
        try {
            log.info("Starting file deletion from Cloudinary. Public ID: {}", publicId);
            
            // Cloudinary's destroy method removes the resource based on its public ID
            Map<?, ?> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            
            if ("ok".equals(deleteResult.get("result"))) {
                log.info("File deleted successfully from Cloudinary.");
            } else {
                log.warn("Cloudinary returned a non-ok result for deletion: {}", deleteResult.get("result"));
            }
            
        } catch (Exception e) {
            log.error("Cloudinary deletion failed for ID: {}. Error: {}", publicId, e.getMessage());
            throw new StorageProcessingException("Failed to delete file from Cloudinary", e);
        }
    }
}
