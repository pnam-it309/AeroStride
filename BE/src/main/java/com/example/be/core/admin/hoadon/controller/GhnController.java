package com.example.be.core.admin.hoadon.controller;

import com.example.be.infrastructure.client.GhnClient;
import com.example.be.infrastructure.constants.RoutesConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({RoutesConstant.ADMIN + "/ghn", "/api/admin/ghn"})
@CrossOrigin(origins = "*")
public class GhnController {

    @Autowired
    private GhnClient ghnClient;

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return responseOrBadGateway(ghnClient.getProvinces(), "provinces");
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        return responseOrBadGateway(ghnClient.getDistricts(provinceId), "districts");
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        return responseOrBadGateway(ghnClient.getWards(districtId), "wards");
    }

    @GetMapping("/fee")
    public ResponseEntity<?> calculateFee(
            @RequestParam Integer toDistrictId,
            @RequestParam String toWardCode,
            @RequestParam(required = false) Integer weight,
            @RequestParam(required = false) Integer insuranceValue) {
        
        Map<String, Object> fee = ghnClient.calculateShippingFee(toDistrictId, toWardCode, weight, insuranceValue);
        if (fee != null) {
            return ResponseEntity.ok(fee);
        }
        // GHN là dịch vụ ngoài. Trả 200 kèm cờ unavailable để POS không bị đỏ console/đứt luồng bán hàng.
        return ResponseEntity.ok(Map.of(
                "success", false,
                "unavailable", true,
                "total", 0,
                "service_fee", 0,
                "message", "Không tính được phí GHN. Vui lòng kiểm tra token/shopId hoặc cấu hình kho gửi."
        ));
    }

    private ResponseEntity<?> responseOrBadGateway(Map<String, Object> data, String type) {
        if (data != null) {
            return ResponseEntity.ok(data);
        }
        // Không trả dữ liệu giả: FE sẽ fallback sang open-api để hiển thị địa chỉ, nhưng không dùng để tính phí GHN.
        return ResponseEntity.ok(Map.of(
                "success", false,
                "unavailable", true,
                "type", type,
                "data", List.of(),
                "message", "Không tải được dữ liệu GHN. Vui lòng kiểm tra token/shopId."
        ));
    }
}
