package com.example.be.core.admin.banhang.controller;

import com.example.be.core.admin.banhang.model.request.AdminBanHangCheckoutRequest;
import com.example.be.core.admin.banhang.model.request.AdminBanHangHoaDonChiTietRequest;
import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.model.response.AdminBanHangKhachHangResponse;
import com.example.be.core.admin.banhang.model.response.BanHangSanPhamResponse;
import com.example.be.core.admin.banhang.service.AdminBanHangService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.be.infrastructure.constants.VaiTro;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_BAN_HANG)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
/**
 * Module: Bán hàng tại quầy (Admin)
 * Chức năng: Quản lý API phục vụ quá trình bán hàng tại quầy của nhân viên/quản trị viên.
 * Bao gồm tạo hóa đơn chờ, thêm sản phẩm, cập nhật số lượng, áp dụng voucher, chọn khách hàng và thanh toán.
 */
public class AdminBanHangController {

    private final AdminBanHangService adminBanHangService;

    // Lấy danh sách hóa đơn đang ở trạng thái chờ (chưa thanh toán)
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminBanHangHoaDonResponse>>> getHoaDonCho() {
        log.info("Fetching draft/pending counter sale invoices");
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.getHoaDonCho()));
    }

    // Tạo mới một hóa đơn chờ
    @PostMapping
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> createHoaDon() {
        log.info("Creating a new counter sale invoice draft");
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.createHoaDon()));
    }

    // Xóa một hóa đơn chờ theo ID
    @DeleteMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<Void>> deleteHoaDon(@PathVariable String id) {
        log.info("Deleting counter sale invoice draft: {}", id);
        adminBanHangService.deleteHoaDon(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Thêm một sản phẩm (chi tiết hóa đơn) vào hóa đơn chờ
    @PostMapping(RoutesConstant.ADD_PRODUCT)
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> addProduct(
            @PathVariable String id,
            @Valid @RequestBody AdminBanHangHoaDonChiTietRequest request
    ) {
        log.info("Adding product to counter sale invoice {}: {}", id, request);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.addSanPham(id, request)));
    }

    // Cập nhật số lượng của một sản phẩm trong hóa đơn
    @PutMapping(RoutesConstant.UPDATE_QUANTITY)
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> updateQuantity(
            @PathVariable String id,
            @PathVariable String idHdct,
            @RequestParam Integer soLuong
    ) {
        log.info("Updating product quantity in invoice detail {} to {}", idHdct, soLuong);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.updateSoLuong(id, idHdct, soLuong)));
    }

    // Xóa một sản phẩm khỏi hóa đơn
    @DeleteMapping(RoutesConstant.REMOVE_PRODUCT)
    public ResponseEntity<ApiResponse<Void>> removeProduct(@PathVariable String id, @PathVariable String idHdct) {
        log.info("Removing product from counter sale invoice: id={}, detailId={}", id, idHdct);
        adminBanHangService.removeHoaDonChiTiet(id, idHdct);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Gắn thông tin khách hàng vào hóa đơn
    @PutMapping(RoutesConstant.KHACH_HANG_SUB)
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> setKhachHang(
            @PathVariable String id,
            @RequestParam(required = false) String idKhachHang
    ) {
        log.info("Setting customer for invoice {}: {}", id, idKhachHang);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.setKhachHang(id, idKhachHang)));
    }

    // Áp dụng hoặc gỡ bỏ mã giảm giá (voucher) cho hóa đơn
    @PutMapping(RoutesConstant.VOUCHER_SUB)
    public ResponseEntity<ApiResponse<AdminBanHangHoaDonResponse>> setVoucher(
            @PathVariable String id,
            @RequestParam(required = false) String idVoucher
    ) {
        log.info("Setting voucher for invoice {}: {}", id, idVoucher);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.setPhieuGiamGia(id, idVoucher)));
    }

    // Thực hiện thanh toán và hoàn tất hóa đơn
    @PostMapping(RoutesConstant.CHECKOUT)
    public ResponseEntity<ApiResponse<Void>> checkout(
            @PathVariable String id,
            @Valid @RequestBody AdminBanHangCheckoutRequest request
    ) {
        log.info("Finalizing checkout for counter sale invoice {}: {}", id, request);
        adminBanHangService.checkout(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Tìm kiếm nhanh sản phẩm để thêm vào hóa đơn
    @GetMapping(RoutesConstant.SEARCH_SAN_PHAM)
    public ResponseEntity<ApiResponse<List<BanHangSanPhamResponse>>> searchSanPham(@RequestParam(defaultValue = "") String keyword) {
        log.info("Searching products for counter sales with keyword: {}", keyword);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.searchSanPham(keyword)));
    }

    // Tìm kiếm khách hàng theo từ khóa (tên, sđt)
    @GetMapping(RoutesConstant.SEARCH_KHACH_HANG)
    public ResponseEntity<ApiResponse<List<AdminBanHangKhachHangResponse>>> searchKhachHang(@RequestParam(defaultValue = "") String keyword) {
        log.info("Searching customers for counter sales with keyword: {}", keyword);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.searchKhachHang(keyword)));
    }

    // Lấy danh sách voucher hợp lệ dựa trên tổng tiền hóa đơn
    @GetMapping(RoutesConstant.VOUCHERS)
    public ResponseEntity<ApiResponse<List<PhieuGiamGia>>> getVouchers(@RequestParam BigDecimal tongTien) {
        log.info("Fetching applicable vouchers for amount: {}", tongTien);
        return ResponseEntity.ok(ApiResponse.success(adminBanHangService.getVouchers(tongTien)));
    }
}
