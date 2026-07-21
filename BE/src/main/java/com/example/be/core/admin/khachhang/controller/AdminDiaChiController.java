package com.example.be.core.admin.khachhang.controller;

import com.example.be.core.admin.khachhang.model.request.AdminDiaChiRequest;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_DIA_CHI)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class AdminDiaChiController {

    private final AdminDiaChiService service;

    @GetMapping(RoutesConstant.KHACH_HANG_DETAIL)
    public ResponseEntity<ApiResponse<?>> getByKhachHang(@PathVariable String khId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByKhachHangId(khId)));
    }

    @PostMapping(RoutesConstant.ADD)
    public ResponseEntity<ApiResponse<?>> add(@Valid @RequestBody AdminDiaChiRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.add(request)));
    }

    @PutMapping(RoutesConstant.UPDATE)
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                  @Valid @RequestBody AdminDiaChiRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request)));
    }

    @DeleteMapping(RoutesConstant.DELETE)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping(RoutesConstant.SET_DEFAULT)
    public ResponseEntity<ApiResponse<Void>> setDefault(@PathVariable String id) {
        service.setDefault(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
