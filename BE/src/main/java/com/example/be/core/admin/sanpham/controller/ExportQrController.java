package com.example.be.core.admin.sanpham.controller;

import com.example.be.core.admin.sanpham.model.request.ExportQrExcelRequest;
import com.example.be.core.admin.sanpham.model.request.ExportQrZipRequest;
import com.example.be.core.admin.sanpham.service.ExportQrService;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.VaiTro;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.ADMIN_SAN_PHAM + "/export")
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class ExportQrController {

    private final ExportQrService exportQrService;

    @PostMapping("/qr-zip")
    public ResponseEntity<byte[]> exportQrZip(@RequestBody ExportQrZipRequest request) throws Exception {
        byte[] data = exportQrService.exportQrZip(request);
        String fileName = request.getFileName() != null ? request.getFileName() : "qr_codes.zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(data);
    }

    @PostMapping("/qr-excel")
    public ResponseEntity<byte[]> exportQrExcel(@RequestBody ExportQrExcelRequest request) throws Exception {
        byte[] data = exportQrService.exportQrExcel(request);
        String fileName = request.getFileName() != null ? request.getFileName() : "qr_codes.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
