package com.example.be.core.common.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.CONFIG_SHIPPING)
public class ShippingConfigController {

    private static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal EXTRA_FEE_PER_KM = new BigDecimal("5000");

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShippingConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("freeShipThreshold", FREE_SHIP_THRESHOLD);
        config.put("extraFeePerKm", EXTRA_FEE_PER_KM);
        // Cấu hình cơ bản (optional)
        config.put("baseFee", new BigDecimal("30000"));
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateShippingFee(
            @RequestParam(required = false) Double distance,
            @RequestParam(required = false) BigDecimal totalAmount
    ) {
        BigDecimal fee = BigDecimal.ZERO;
        
        // 1. Kiểm tra miễn phí vận chuyển theo giá trị đơn hàng
        if (totalAmount != null && totalAmount.compareTo(FREE_SHIP_THRESHOLD) >= 0) {
            Map<String, Object> result = new HashMap<>();
            result.put("fee", fee);
            result.put("message", "Miễn phí vận chuyển cho đơn hàng từ 500k");
            return ResponseEntity.ok(ApiResponse.success(result));
        }

        // 2. Tính phí dựa trên khoảng cách
        if (distance != null && distance > 0) {
            if (distance <= 5.0) {
                fee = new BigDecimal("30000"); // Dưới 5km: 30k
            } else {
                // Trên 5km: 30k + 5k/km phát sinh
                BigDecimal extraDistance = BigDecimal.valueOf(distance - 5.0);
                BigDecimal extraFee = extraDistance.multiply(EXTRA_FEE_PER_KM);
                fee = new BigDecimal("30000").add(extraFee);
            }
        } else {
            // Mặc định nếu không có distance
            fee = new BigDecimal("30000");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("fee", fee);
        result.put("message", "Phí vận chuyển tính theo khoảng cách");
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
