package com.example.be.core.storage.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.storage.StorageService;
import com.example.be.core.storage.dto.FileUploadResult;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

/**
 * Storage chung. Ảnh sản phẩm/avatar hiện được lưu trực tiếp vào DB dưới dạng base64
 * (xử lý phía FE), nên endpoint upload ở đây chỉ còn là fallback lưu file cục bộ
 * (LocalStorageServiceImpl) cho các luồng còn dùng multipart.
 */
@RestController
@RequestMapping(RoutesConstant.STORAGE)
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @GetMapping("/files")
    public ResponseEntity<ApiResponse<Object>> listFiles() {
        return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResult>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "aerostride/general") String folder) {
        return ResponseEntity.ok(ApiResponse.success(storageService.uploadFile(file, folder)));
    }

    @DeleteMapping("/files/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String publicId) {
        storageService.deleteFile(publicId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
