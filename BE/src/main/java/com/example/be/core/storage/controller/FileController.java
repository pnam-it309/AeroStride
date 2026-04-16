package com.example.be.core.storage.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(RoutesConstant.API_PREFIX + "/storage")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<Object>> listFiles() {
        // Placeholder returning empty list to avoid 500 error
        return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadResult>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "aerostride/general") String folder) {
        
        FileUploadResult result = storageService.uploadFile(file, folder);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/files/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String publicId) {
        storageService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
