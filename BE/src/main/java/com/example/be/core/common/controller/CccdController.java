package com.example.be.core.common.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.CccdDto;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.utils.CccdParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.API_PREFIX + "/common/cccd")
@RequiredArgsConstructor
public class CccdController {

    @PostMapping("/parse")
    public ResponseEntity<ApiResponse<CccdDto>> parseQrCode(@RequestBody Map<String, String> request) {
        String qrData = request.get("qrData");
        CccdDto result = CccdParser.parse(qrData);
        
        if (result == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Failed to parse CCCD QR code data"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
