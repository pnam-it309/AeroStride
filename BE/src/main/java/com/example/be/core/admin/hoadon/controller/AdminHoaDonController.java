package com.example.be.core.admin.hoadon.controller;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
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

    @GetMapping
    public ResponseEntity<?> getPage(AdminHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.getPage(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.detail(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateStatus(id, status)));
    }

    @GetMapping("/counts")
    public ResponseEntity<?> getCounts() {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.getCounts()));
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(AdminHoaDonRequest request) {
        byte[] excelContent = adminHoaDonService.exportExcel(request);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_hoa_don.xlsx")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(excelContent);
    }
    @GetMapping("/{id}/print")
    public ResponseEntity<String> printInvoice(@PathVariable String id) {
        String html = adminHoaDonService.generateInvoiceHtml(id);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
