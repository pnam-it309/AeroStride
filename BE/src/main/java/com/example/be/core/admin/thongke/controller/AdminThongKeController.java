package com.example.be.core.admin.thongke.controller;

import com.example.be.core.admin.thongke.model.response.AdminThongKeResponse;
import com.example.be.core.admin.thongke.service.AdminThongKeService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_THONG_KE)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class AdminThongKeController {

    private final AdminThongKeService adminThongKeService;

    @GetMapping("/tong-quan")
    public ResponseEntity<ApiResponse<AdminThongKeResponse>> tongQuan(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        log.info("Fetching statistics overview from {} to {}", tuNgay, denNgay);
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getTongQuan(tuNgay, denNgay)));
    }

    @GetMapping("/doanh-thu-theo-ngay")
    public ResponseEntity<ApiResponse<List<?>>> doanhThuTheoNgay(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        log.info("Fetching daily revenue details from {} to {}", tuNgay, denNgay);
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getDoanhThuTheoNgay(tuNgay, denNgay)));
    }

    @GetMapping("/don-hang-gan-day")
    public ResponseEntity<ApiResponse<List<?>>> donHangGanDay(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Fetching recent orders with limit: {}", limit);
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getDonHangGanDay(limit)));
    }

    @GetMapping("/san-pham")
    public ResponseEntity<ApiResponse<com.example.be.core.common.dto.PageResponse<AdminThongKeResponse.SanPhamBanChay>>> getProductStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "bestSelling") String sortBy) {
        log.info("Fetching product statistics from {} to {}, keyword: {}, page: {}, size: {}, sortBy: {}", tuNgay, denNgay, keyword, page, size, sortBy);
        return ResponseEntity.ok(ApiResponse.success(adminThongKeService.getProductStatistics(tuNgay, denNgay, keyword, page, size, sortBy)));
    }
}
