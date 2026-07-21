package com.example.be.core.admin.khachhang.controller;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageResponse;
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

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_KHACH_HANG)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class AdminKhachHangController {

    private final AdminKhachHangService adminKhachHangService;

    @GetMapping({RoutesConstant.HIEN_THI, RoutesConstant.PHAN_TRANG, RoutesConstant.TIM_KIEM}) // Aliases for FE compatibility
    public ResponseEntity<ApiResponse<PageResponse<AdminKhachHangResponse>>> search(AdminKhachHangRequest request) {
        // Trả PageResponse chuẩn (giống các module khác) thay vì Spring Page thô, để FE chỉ đọc 1 shape.
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(adminKhachHangService.search(request))));
    }

    @GetMapping(RoutesConstant.DETAIL) // Compatibility Alias
    public ResponseEntity<ApiResponse<AdminKhachHangResponse>> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.detail(id)));
    }

    @PostMapping(RoutesConstant.ADD) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> add(@Valid @RequestBody AdminKhachHangRequest request) {
        adminKhachHangService.add(request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.KHACH_HANG_ADD_SUCCESS));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@PathVariable String id,
                                                     @Valid @RequestBody AdminKhachHangRequest request) {
        adminKhachHangService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.KHACH_HANG_UPDATE_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminKhachHangService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.KHACH_HANG_DELETE_SUCCESS));
    }

    @PutMapping(RoutesConstant.STATUS) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id,
                                                           @Valid @RequestBody UpdateStatusRequest body) {
        adminKhachHangService.doiTrangThai(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.UPDATE_STATUS_SUCCESS));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=khach_hang.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(adminKhachHangService.exportExcel());
    }
}
