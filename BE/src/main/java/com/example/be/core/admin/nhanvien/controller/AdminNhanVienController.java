package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(RoutesConstant.ADMIN_NHAN_VIEN)
@RequiredArgsConstructor
public class AdminNhanVienController {

    private final AdminNhanVienService adminNhanVienService;

    @GetMapping({RoutesConstant.HIEN_THI, RoutesConstant.PHAN_TRANG, RoutesConstant.TIM_KIEM}) // Aliases for FE compatibility
    public ResponseEntity<ApiResponse<?>> search(AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.search(request)));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.detail(id)));
    }

    @PostMapping(RoutesConstant.ADD) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> add(@RequestBody AdminNhanVienRequest request) {
        adminNhanVienService.add(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Thêm nhân viên thành công!"));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String id, @RequestBody AdminNhanVienRequest request) {
        adminNhanVienService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật nhân viên thành công!"));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminNhanVienService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa nhân viên thành công!"));
    }

    @PatchMapping(RoutesConstant.STATUS_ALT)
    @PutMapping(RoutesConstant.STATUS) // Compatibility Alias (Old FE uses PUT for status)
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        TrangThai status = TrangThai.valueOf(body.get("status"));
        adminNhanVienService.doiTrangThai(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
    }

    @PostMapping(RoutesConstant.AVATAR)
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.uploadAvatar(file)));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=nhan_vien.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(adminNhanVienService.exportExcel());
    }

    @GetMapping(RoutesConstant.PHAN_QUYEN)
    public ResponseEntity<ApiResponse<?>> layDanhSachPhanQuyen() {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.getAllPhanQuyen()));
    }
}
