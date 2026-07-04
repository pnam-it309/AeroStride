package com.example.be.core.admin.hoadon.controller;

import com.example.be.infrastructure.client.GhnClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/ghn")
@CrossOrigin(origins = "*")
public class GhnController {

    @Autowired
    private GhnClient ghnClient;

    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        return ResponseEntity.ok(ghnClient.getProvinces());
    }

    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ghnClient.getDistricts(provinceId));
    }

    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ghnClient.getWards(districtId));
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
        return ResponseEntity.badRequest().body(Map.of("message", "Could not calculate fee"));
    }
}
