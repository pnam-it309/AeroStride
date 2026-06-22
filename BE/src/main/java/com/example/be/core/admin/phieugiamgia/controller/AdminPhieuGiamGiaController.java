package com.example.be.core.admin.phieugiamgia.controller;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
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
@RequestMapping(RoutesConstant.ADMIN_PHIEU_GIAM_GIA)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
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
    public ResponseEntity<ApiResponse<Void>> add(@Valid @RequestBody AdminPhieuGiamGiaRequest req) {
        service.add(req);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.PHIEU_GIAM_GIA_ADD_SUCCESS));
    }

    @PutMapping(RoutesConstant.UPDATE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> update(@Valid @RequestBody AdminPhieuGiamGiaRequest req,
                                                     @PathVariable String id) {
        service.update(req, id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.PHIEU_GIAM_GIA_UPDATE_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.DELETE) // Compatibility Alias
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.PHIEU_GIAM_GIA_DELETE_SUCCESS));
    }

    @PatchMapping(RoutesConstant.STATUS_ALT)
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable String id,
                                                           @Valid @RequestBody UpdateStatusRequest body) {
        service.updateStatus(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.UPDATE_STATUS_SUCCESS));
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
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.IMPORT_SUCCESS));
    }
}
