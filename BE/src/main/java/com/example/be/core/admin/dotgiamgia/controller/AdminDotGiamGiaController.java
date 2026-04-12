package com.example.be.core.admin.dotgiamgia.controller;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaSearchRequest;
import com.example.be.core.admin.dotgiamgia.service.AdminDotGiamGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;

@RestController
@RequestMapping(RoutesConstant.ADMIN_DOT_GIAM_GIA)
@CrossOrigin("*")
@RequiredArgsConstructor
public class AdminDotGiamGiaController {

    @Autowired
    private AdminDotGiamGiaService service;

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.findById(id)));
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(
        @RequestParam(defaultValue = "0") Integer pageNo,
        @RequestParam(defaultValue = "5") Integer pageSize,
        @RequestParam(defaultValue = "") String keyword
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.phanTrang(pageNo, pageSize, keyword)));
    }

    @PostMapping("/add")
    public void add(@RequestBody AdminDotGiamGiaRequest req) {
        service.add(req);
    }

    @PutMapping("/update")
    public void update(@RequestBody AdminDotGiamGiaRequest req,
                       @RequestParam String id) {
        service.update(req, id);
    }

    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam com.example.be.infrastructure.constants.TrangThai status) {
        service.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!"));
    }

    @DeleteMapping("/delete")
    public void delete(@RequestParam String id) {
        service.delete(id);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelContent = service.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_dot_giam_gia.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }
 
    @GetMapping("/san-pham-ap-dung")
    public ResponseEntity<?> layDanhSachSanPhamApDung() {
        return ResponseEntity.ok(ApiResponse.success(service.getAvailableVariants()));
    }
 
    @GetMapping("/bien-the-ap-dung/{id}")
    public ResponseEntity<?> layDanhSachBienTheApDung(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getAppliedVariants(id)));
    }
}
