package com.example.be.core.admin.hoadon.controller;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_HOA_DON)
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminHoaDonController {

    private final AdminHoaDonService adminHoaDonService;

    @GetMapping({RoutesConstant.HIEN_THI, RoutesConstant.PHAN_TRANG}) // Aliases for FE compatibility
    public ResponseEntity<ApiResponse<?>> getPage(AdminHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.getPage(request)));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<?>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.detail(id)));
    }

    @PatchMapping(RoutesConstant.STATUS_ALT)
    public ResponseEntity<ApiResponse<?>> updateStatus(@PathVariable String id, @RequestParam Integer status, @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateStatus(id, status, note)));
    }

    @PutMapping(RoutesConstant.INFO)
    public ResponseEntity<ApiResponse<?>> updateInfo(@PathVariable String id, @RequestBody AdminUpdateHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateInfo(id, request)));
    }

    @PutMapping(RoutesConstant.PRODUCTS)
    public ResponseEntity<ApiResponse<?>> updateHdct(@PathVariable String id, @RequestBody AdminUpdateHdctRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateHdct(id, request)));
    }

    @DeleteMapping(RoutesConstant.PRODUCTS_DETAIL)
    public ResponseEntity<ApiResponse<?>> removeHdct(@PathVariable String id, @PathVariable String idHdct) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.removeHdct(id, idHdct)));
    }

    @GetMapping(RoutesConstant.COUNTS)
    public ResponseEntity<ApiResponse<?>> getCounts(AdminHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.getCounts(request)));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel(AdminHoaDonRequest request) {
        byte[] excelContent = adminHoaDonService.exportExcel(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_hoa_don.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(excelContent);
    }

    @GetMapping(RoutesConstant.PRINT)
    public ResponseEntity<String> printInvoice(@PathVariable String id) {
        String html = adminHoaDonService.generateInvoiceHtml(id);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
