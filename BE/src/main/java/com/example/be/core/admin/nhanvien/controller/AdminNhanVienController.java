package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.UpdateStatusRequest;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_NHAN_VIEN)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_ONLY)
public class AdminNhanVienController {

    private final AdminNhanVienService adminNhanVienService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.getMe()));
    }

    @GetMapping({RoutesConstant.HIEN_THI, RoutesConstant.PHAN_TRANG, RoutesConstant.TIM_KIEM}) // Aliases for FE compatibility
    public ResponseEntity<ApiResponse<?>> search(AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.search(request)));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.detail(id)));
    }

    @PostMapping(RoutesConstant.ADD) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> add(@Valid @RequestBody AdminNhanVienRequest request) {
        adminNhanVienService.add(request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.NHAN_VIEN_ADD_SUCCESS));
    }

    @PostMapping("/{id}/register-face")
    public ResponseEntity<ApiResponse<Void>> registerFace(@PathVariable String id, @RequestParam("image") MultipartFile image) {
        adminNhanVienService.registerFace(id, image);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng ký khuôn mặt thành công!"));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String id,
                                                     @Valid @RequestBody AdminNhanVienRequest request) {
        adminNhanVienService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.NHAN_VIEN_UPDATE_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminNhanVienService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.NHAN_VIEN_DELETE_SUCCESS));
    }

    @PatchMapping(RoutesConstant.STATUS_ALT)
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id,
                                                           @Valid @RequestBody UpdateStatusRequest body) {
        adminNhanVienService.doiTrangThai(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.UPDATE_STATUS_SUCCESS));
    }

    @PostMapping(RoutesConstant.AVATAR)
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.uploadAvatar(file)));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nhan_vien.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(adminNhanVienService.exportExcel());
    }

    @GetMapping(RoutesConstant.PHAN_QUYEN)
    public ResponseEntity<ApiResponse<?>> layDanhSachPhanQuyen() {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.getAllPhanQuyen()));
    }
}
