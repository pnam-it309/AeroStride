package com.example.be.core.admin.phieugiamgia.controller;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.ADMIN_PHIEU_GIAM_GIA)
@RequiredArgsConstructor
public class AdminPhieuGiamGiaController {

    private final AdminPhieuGiamGiaService service;

    @GetMapping({RoutesConstant.HIEN_THI, RoutesConstant.PHAN_TRANG}) // Compatibility Aliases
    public ResponseEntity<ApiResponse<?>> search(AdminPhieuGiamGiaRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.phanTrang(request)));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.detail(id)));
    }

    @PostMapping(RoutesConstant.ADD) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> add(@RequestBody AdminPhieuGiamGiaRequest req) {
        service.add(req);
        return ResponseEntity.ok(ApiResponse.success(null, "Thêm thành công!"));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@RequestBody AdminPhieuGiamGiaRequest req, @PathVariable String id) {
        service.update(req, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật thành công!"));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thành công!"));
    }

    @RequestMapping(value = {"/{id}/status", "/status/{id}"}, method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "ERR_INVALID_STATUS", "Trạng thái không được để trống!", null, null));
        }
        
        try {
            TrangThai status = TrangThai.valueOf(statusStr.trim().toUpperCase());
            service.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "ERR_INVALID_STATUS_VALUE", "Trạng thái không hợp lệ: " + statusStr, null, null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(500, "ERR_INTERNAL", "Lỗi xử lý cập nhật trạng thái: " + e.getMessage(), null, null));
        }
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelContent = service.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_phieu_giam_gia.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }

    @GetMapping(RoutesConstant.DOWNLOAD_TEMPLATE)
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = service.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_voucher.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping(RoutesConstant.IMPORT_EXCEL)
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") MultipartFile file) {
        service.importExcel(file);
        return ResponseEntity.ok(ApiResponse.success(null, "Import thành công!"));
    }
}
