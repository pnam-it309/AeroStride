package com.example.be.core.storage;

import com.example.be.core.storage.dto.FileUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Uploads a file to the storage provider.
     * @param file The file to upload.
     * @param folderName The target folder in the storage.
     * @return FileUploadResult containing url, public id, and format.
     */
    FileUploadResult uploadFile(MultipartFile file, String folderName);

    /**
     * Deletes a file from the storage provider using its public ID.
     * @param publicId The unique identifier of the file in the storage.
     */
    void deleteFile(String publicId);
}
