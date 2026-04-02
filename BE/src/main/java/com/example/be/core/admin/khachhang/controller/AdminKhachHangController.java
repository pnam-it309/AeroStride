package com.example.be.core.admin.khachhang.controller;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.service.AdminKhachHangService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_KHACH_HANG)
@RequiredArgsConstructor
public class AdminKhachHangController {
    @Autowired
    private AdminKhachHangService adminKhachHangService;

    @GetMapping("/hien-thi")
    public ResponseEntity<?> hienThi() {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.hienThi()));
    }
    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(AdminKhachHangRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.phanTrang(request)));
    }

    @GetMapping("/tim-kiem")
    public ResponseEntity<?> timKiem(AdminKhachHangRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.timKiem(request)));
    }

    @GetMapping("/filter")
    public ResponseEntity<?> locKH(AdminKhachHangRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.locKH(req)));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.detail(id)));
    }

    @PostMapping("/add")
    public ResponseEntity<?> create(@RequestBody AdminKhachHangRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.add(request)));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody AdminKhachHangRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminKhachHangService.update(id, request)));
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable String id,
                                          @RequestParam TrangThai trangThai) {
        adminKhachHangService.doiTrangThai(id, trangThai);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!"));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        adminKhachHangService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
    }


}
