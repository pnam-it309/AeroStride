package com.example.be.core.admin.banhang.controller;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.service.AdminBanHangService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(RoutesConstant.ADMIN_BAN_HANG)
@RequiredArgsConstructor
@CrossOrigin("*") // Enable CORS for development
public class AdminBanHangController {

    private final AdminBanHangService adminBanHangService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBanHangHoaDonResponse>>> getHoaDonCho() {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.getHoaDonCho()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> createHoaDon() {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.createHoaDon()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoaDon(@PathVariable String id) {
        adminBanHangService.deleteHoaDon(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/add-product")
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> addProduct(@PathVariable String id, @RequestBody AdminBanHangHoaDonChiTietRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.addSanPham(id, request)));
    }

    @PutMapping("/{id}/update-quantity/{idHdct}")
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> updateQuantity(
            @PathVariable String id,
            @PathVariable String idHdct,
            @RequestParam Integer soLuong) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.updateSoLuong(id, idHdct, soLuong)));
    }

    @DeleteMapping("/{id}/remove-product/{idHdct}")
    public ResponseEntity<ApiResponse<Void>> removeProduct(@PathVariable String id, @PathVariable String idHdct) {
        adminBanHangService.removeHoaDonChiTiet(id, idHdct);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/khach-hang")
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> setKhachHang(@PathVariable String id, @RequestParam(required = false) String idKhachHang) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.setKhachHang(id, idKhachHang)));
    }

    @PutMapping("/{id}/voucher")
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> setVoucher(@PathVariable String id, @RequestParam(required = false) String idVoucher) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.setPhieuGiamGia(id, idVoucher)));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<ApiResponse<Void>> checkout(@PathVariable String id, @RequestBody AdminBanHangCheckoutRequest request) {
        adminBanHangService.checkout(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/search-san-pham")
    public ResponseEntity<ApiResponse<List<BanHangSanPhamResponse>>> searchSanPham(@RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.searchSanPham(keyword)));
    }

    @GetMapping("/search-khach-hang")
    public ResponseEntity<ApiResponse<List<AdminBanHangKhachHangResponse>>> searchKhachHang(@RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.searchKhachHang(keyword)));
    }

    @GetMapping("/vouchers")
    public ResponseEntity<ApiResponse<List<PhieuGiamGia>>> getVouchers(@RequestParam BigDecimal tongTien) {
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.getVouchers(tongTien)));
    }
}
