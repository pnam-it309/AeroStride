package com.example.be.core.admin.hoadon.controller;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHdctRequest;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.core.common.dto.ApiResponse;
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

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_HOA_DON)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
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
    public ResponseEntity<ApiResponse<?>> updateStatus(@PathVariable String id,
                                                        @RequestParam Integer status,
                                                        @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateStatus(id, status, note)));
    }

    @PutMapping(RoutesConstant.INFO)
    public ResponseEntity<ApiResponse<?>> updateInfo(@PathVariable String id,
                                                      @Valid @RequestBody AdminUpdateHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateInfo(id, request)));
    }

    @PutMapping(RoutesConstant.PRODUCTS)
    public ResponseEntity<ApiResponse<?>> updateHdct(@PathVariable String id,
                                                      @Valid @RequestBody AdminUpdateHdctRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateHdct(id, request)));
    }

    @DeleteMapping(RoutesConstant.PRODUCTS_DETAIL)
    public ResponseEntity<ApiResponse<?>> removeHdct(@PathVariable String id, @PathVariable String idHdct) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.removeHdct(id, idHdct)));
    }

    // Xác nhận đã hoàn phí cho khách hàng (đơn trả trước bị hủy)
    @PatchMapping(RoutesConstant.REFUND_CONFIRM)
    public ResponseEntity<ApiResponse<?>> confirmRefund(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.confirmRefund(id), "Đã xác nhận hoàn phí"));
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
