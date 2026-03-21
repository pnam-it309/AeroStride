package com.example.be.core.storage.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(RoutesConstant.API_PREFIX + "/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResult>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "aerostride/general") String folder) {
        
        FileUploadResult result = storageService.uploadFile(file, folder);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String publicId) {
        storageService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
