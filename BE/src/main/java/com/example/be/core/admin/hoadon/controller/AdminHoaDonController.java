package com.example.be.core.admin.hoadon.controller;

import com.example.be.core.admin.hoadon.model.request.AdminHoaDonRequest;
import com.example.be.core.admin.hoadon.service.AdminHoaDonService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_HOA_DON)
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminHoaDonController {

    private final AdminHoaDonService adminHoaDonService;

    @GetMapping
    public ResponseEntity<?> getPage(AdminHoaDonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.getPage(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.detail(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam Integer status) {
        return ResponseEntity.ok(ApiResponse.success(adminHoaDonService.updateStatus(id, status)));
    }
}
