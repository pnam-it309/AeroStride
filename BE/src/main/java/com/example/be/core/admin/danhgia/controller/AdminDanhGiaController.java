package com.example.be.core.admin.danhgia.controller;

import com.example.be.core.admin.danhgia.model.request.AdminDanhGiaFilterRequest;
import com.example.be.core.admin.danhgia.model.response.AdminDanhGiaResponse;
import com.example.be.core.admin.danhgia.service.AdminDanhGiaService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.entity.DanhGiaSanPham;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_DANH_GIA)
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminDanhGiaController {

    private final AdminDanhGiaService service;

    @GetMapping
    public ResponseEntity<ApiResponse> getDanhGiaList(
            @ModelAttribute AdminDanhGiaFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayTao") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AdminDanhGiaResponse> responsePage = service.getPageDanhGia(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(responsePage));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable String id,
            @RequestParam DanhGiaSanPham.TrangThaiDanhGia trangThai) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStatus(id, trangThai)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteDanhGia(@PathVariable String id) {
        service.deleteDanhGia(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công"));
    }
}
