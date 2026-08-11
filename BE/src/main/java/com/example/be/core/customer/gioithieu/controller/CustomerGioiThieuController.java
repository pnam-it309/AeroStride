package com.example.be.core.customer.gioithieu.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.repository.SanPhamRepository;
import com.example.be.repository.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER_GIOI_THIEU)
@RequiredArgsConstructor
public class CustomerGioiThieuController {

    private final SanPhamRepository sanPhamRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAboutStats() {
        long totalProducts = sanPhamRepository.count();
        long totalBrands = thuongHieuRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts > 0 ? totalProducts : 500);
        stats.put("totalBrands", totalBrands > 0 ? totalBrands : 12);
        stats.put("totalShowrooms", 50);
        stats.put("satisfactionRate", "99.8%");

        return ResponseEntity.ok(ApiResponse.success(stats, "Lấy chỉ số thống kê thành công"));
    }
}
