package com.example.be.core.common.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.jobs.KeepAliveJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.CRON)
@RequiredArgsConstructor
public class CronController {

    private final KeepAliveJob keepAliveJob;

    /**
     * Endpoint công khai cho phép dịch vụ cron bên ngoài (hoặc curl) gọi vào để trigger job hoặc ping backend.
     * Hỗ trợ cả GET và POST tại: /api/v1/cron/ping
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pingGet() {
        return handlePing();
    }

    @PostMapping("/ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pingPost() {
        return handlePing();
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> handlePing() {
        keepAliveJob.execute();
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("executedAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success(result, "Trigger cronjob thành công"));
    }
}
