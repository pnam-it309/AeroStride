package com.example.be.core.admin.giaoca.controller;

import com.example.be.core.admin.giaoca.model.request.AdminChotCaRequest;
import com.example.be.core.admin.giaoca.model.request.AdminMoCaRequest;
import com.example.be.core.admin.giaoca.service.AdminGiaoCaService;
import com.example.be.core.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/giao-ca")
public class AdminGiaoCaController {

    @Autowired
    private AdminGiaoCaService giaoCaService;

    @PostMapping("/mo-ca")
    public ApiResponse<?> moCa(@RequestBody AdminMoCaRequest request) {
        return ApiResponse.success(giaoCaService.moCa(request));
    }

    @GetMapping("/hien-tai")
    public ApiResponse<?> getCaHienTai(Authentication authentication) {
        return ApiResponse.success(giaoCaService.getCaHienTai(authentication.getName()));
    }

    @PutMapping("/chot-ca")
    public ApiResponse<?> chotCa(Authentication authentication, @RequestBody AdminChotCaRequest request) {
        return ApiResponse.success(giaoCaService.chotCa(authentication.getName(), request));
    }

    @GetMapping
    public ApiResponse<?> getAll() {
        return ApiResponse.success(giaoCaService.getAllLichSu());
    }
}
