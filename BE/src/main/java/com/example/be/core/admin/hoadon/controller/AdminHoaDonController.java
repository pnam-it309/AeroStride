package com.example.be.core.admin.hoadon.controller;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_HOA_DON)
@RequiredArgsConstructor
@CrossOrigin("*") // Để Vue.js có thể gọi API mà không bị lỗi CORS
public class AdminHoaDonController {

    private final AdminHoaDonService adminHoaDonService;

    /**
     * API Lấy danh sách hóa đơn kèm phân trang và bộ lọc
     * URL ví dụ: /admin/hoa-don?page=0&size=10&search=HD01&trangThai=1
     */
    @GetMapping
    public ResponseEntity<?> getPage(AdminHoaDonRequest request) {
        return ResponseEntity.ok(adminHoaDonService.getPage(request));
    }

    /**
     * API Lấy chi tiết một hóa đơn
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(adminHoaDonService.detail(id));
    }

    /**
     * API Cập nhật trạng thái hóa đơn
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        return ResponseEntity.ok(adminHoaDonService.updateStatus(id, status));
    }
}
