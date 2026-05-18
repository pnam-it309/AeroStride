package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.admin.nhanvien.service.ResetPasswordService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.RESET_PASSWORD)
@RequiredArgsConstructor
public class ResetPasswordRequestController {

    private final ResetPasswordService resetPasswordService;

    @PostMapping(RoutesConstant.REQUEST)
    public ResponseEntity<ApiResponse<Void>> requestReset(@RequestParam String email) {
        resetPasswordService.requestReset(email);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.RESET_PASSWORD_REQUEST_SUCCESS));
    }

    @GetMapping(RoutesConstant.PENDING)
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public ResponseEntity<ApiResponse<?>> getPendingRequests() {
        return ResponseEntity.ok(ApiResponse.success(resetPasswordService.getPendingRequests()));
    }

    @PostMapping(RoutesConstant.APPROVE)
    @PreAuthorize("hasRole('QUAN_TRI_VIEN')")
    public ResponseEntity<ApiResponse<Void>> approveReset(@PathVariable String id) {
        resetPasswordService.approveReset(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.RESET_PASSWORD_APPROVE_SUCCESS));
    }
}
