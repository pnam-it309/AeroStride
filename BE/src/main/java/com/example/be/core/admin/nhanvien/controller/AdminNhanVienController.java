package com.example.be.core.admin.nhanvien.controller;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.service.AdminNhanVienService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageRequest;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RoutesConstant.ADMIN_NHAN_VIEN)
@RequiredArgsConstructor
public class AdminNhanVienController {
    @Autowired
    private AdminNhanVienService adminNhanVienService;

    @GetMapping("/hien-thi")
    public List<AdminNhanVienResponse> hienThi() {
        return adminNhanVienService.hienThi();
    }

    @GetMapping("/phan-trang")
    public ResponseEntity<?> phanTrang(AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.phanTrang(request)));
    }
    @GetMapping("/tim-kiem")
    public ResponseEntity<?> timKiem(AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.timKiem(request)));
    }
    @GetMapping("/filter")
    public ResponseEntity<?> filter(AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.locNV(request)));
    }
    @GetMapping("/detail/{id}")
    public ResponseEntity<?> detail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.detail(id)));
    }
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.add(request)));
    }
    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                    @RequestBody AdminNhanVienRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminNhanVienService.update(id,request)));
    }
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable String id,
                                          @RequestParam TrangThai trangThai){
        adminNhanVienService.doiTrangThai(id,trangThai);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công!"));
    }
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        adminNhanVienService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thành công!"));
    }



}
