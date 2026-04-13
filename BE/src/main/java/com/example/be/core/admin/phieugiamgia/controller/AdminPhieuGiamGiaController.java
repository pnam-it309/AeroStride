package com.example.be.core.admin.phieugiamgia.controller;

import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_PHIEU_GIAM_GIA)
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminPhieuGiamGiaController {

    private final AdminPhieuGiamGiaService service;

    @GetMapping("/hien-thi")
    public ResponseEntity<?> hienThi() {
        return ResponseEntity.ok(ApiResponse.success(service.hienThi()));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.detail(id)));
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "5", required = false) Integer pageSize,
            @RequestParam(value = "keyword", defaultValue = "", required = false) String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.phanTrang(pageNo, pageSize, keyword)));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AdminPhieuGiamGiaRequest req) {
        service.add(req);
        return ResponseEntity.ok(ApiResponse.success("Thêm thành công!"));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@RequestBody AdminPhieuGiamGiaRequest req,
                                    @PathVariable String id) {
        service.update(req, id);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công!"));
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam com.example.be.infrastructure.constants.TrangThai status) {
        service.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!"));
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelContent = service.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_phieu_giam_gia.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }
    @GetMapping("/download-template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = service.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_voucher.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/import-excel")
    public ResponseEntity<?> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        service.importExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Import thành công!"));
    }
}
