package com.example.be.core.admin.khachhang.controller;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_KHACH_HANG)
@RequiredArgsConstructor
public class AdminKhachHangController {

    private final AdminKhachHangService adminKhachHangService;

    @GetMapping({"/hien-thi", "/phan-trang", "/tim-kiem"}) // Aliases for FE compatibility
    public ResponseEntity<ApiResponse<?>> search(AdminKhachHangRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.search(request)));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.detail(id)));
    }

    @PostMapping(RoutesConstant.ADD) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> add(@RequestBody AdminKhachHangRequest request) {
        adminKhachHangService.add(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Thêm khách hàng thành công!"));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String id, @RequestBody AdminKhachHangRequest request) {
        adminKhachHangService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật khách hàng thành công!"));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminKhachHangService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa khách hàng thành công!"));
    }

    @PutMapping(RoutesConstant.STATUS) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id, @RequestBody java.util.Map<String, String> body) {
        TrangThai status = TrangThai.valueOf(body.get("status"));
        adminKhachHangService.doiTrangThai(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=khach_hang.xlsx")
                .contentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM)
                .body(adminKhachHangService.exportExcel());
    }
}
