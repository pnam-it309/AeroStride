package com.example.be.core.admin.dotgiamgia.controller;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaSearchRequest;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_DOT_GIAM_GIA)
@RequiredArgsConstructor
public class AdminDotGiamGiaController {

    private final AdminDotGiamGiaService service;

    @GetMapping(RoutesConstant.PHAN_TRANG) // Alias for Frontend backward compatibility
    public ResponseEntity<ApiResponse<?>> search(AdminDotGiamGiaSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.search(request)));
    }

    @GetMapping(RoutesConstant.DETAIL)
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.findById(id)));
    }
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> add(@RequestBody AdminDotGiamGiaRequest req) {
        service.add(req);
        return ResponseEntity.ok(ApiResponse.success(null, "Thêm đợt giảm giá thành công!"));
    }

    @PutMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody AdminDotGiamGiaRequest req,
                                                     @PathVariable String id) {
        service.update(req, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật đợt giảm giá thành công!"));
    }

    @PatchMapping(RoutesConstant.STATUS_SUB)
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id, @RequestParam TrangThai status) {
        service.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
    }

    @DeleteMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa đợt giảm giá thành công!"));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelContent = service.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_dot_giam_gia.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }

    @GetMapping(RoutesConstant.SAN_PHAM_AP_DUNG)
    public ResponseEntity<ApiResponse<?>> layDanhSachSanPhamApDung() {
        return ResponseEntity.ok(ApiResponse.success(service.getAvailableVariants()));
    }

    @GetMapping(RoutesConstant.BIEN_THE_AP_DUNG)
    public ResponseEntity<ApiResponse<?>> layDanhSachBienTheApDung(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getAppliedVariants(id)));
    }
}
