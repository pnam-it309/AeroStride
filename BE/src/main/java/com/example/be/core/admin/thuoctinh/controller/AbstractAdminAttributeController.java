package com.example.be.core.admin.thuoctinh.controller;

import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import com.example.be.core.admin.thuoctinh.model.response.AdminAttributeResponse;
import com.example.be.core.admin.thuoctinh.service.AdminAttributeManagementService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.core.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

public abstract class AbstractAdminAttributeController {

    private final AdminAttributeManagementService service;

    protected AbstractAdminAttributeController(AdminAttributeManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminAttributeResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        PageRequest pageRequest = PageRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        return ResponseEntity.ok(ApiResponse.success(service.search(keyword, trangThai, pageRequest), "Lay danh sach thuoc tinh thanh cong"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminAttributeResponse>> getDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id), "Lay chi tiet thuoc tinh thanh cong"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminAttributeResponse>> create(@Valid @RequestBody AdminAttributeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(request), "Tao thuoc tinh thanh cong"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminAttributeResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody AdminAttributeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, request), "Cap nhat thuoc tinh thanh cong"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa mem thuoc tinh thanh cong"));
    }
}
