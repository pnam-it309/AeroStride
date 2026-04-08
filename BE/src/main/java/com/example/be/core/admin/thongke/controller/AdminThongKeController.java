package com.example.be.core.admin.thongke.controller;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import com.example.be.core.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/thong-ke")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminThongKeController {

    private final AdminThongKeService adminThongKeService;

    @GetMapping("/tong-quan")
    public ResponseEntity<?> tongQuan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getTongQuan(tuNgay, denNgay)));
    }

    @GetMapping("/doanh-thu-theo-ngay")
    public ResponseEntity<?> doanhThuTheoNgay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getDoanhThuTheoNgay(tuNgay, denNgay)));
    }

    @GetMapping("/don-hang-gan-day")
    public ResponseEntity<?> donHangGanDay(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getDonHangGanDay(limit)));
    }
}
