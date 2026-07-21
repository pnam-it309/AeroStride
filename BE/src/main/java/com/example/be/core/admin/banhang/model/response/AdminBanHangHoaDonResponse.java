package com.example.be.core.admin.banhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class AdminBanHangHoaDonResponse {
    private String id;
    private String maHoaDon;
    private String idKhachHang;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String idPhieuGiamGia;
    private String loaiDon;
    private BigDecimal tongTienHang; // Tổng tiền hàng gốc
    private BigDecimal tienGiamGiaSanPham; // Tiền giảm do đợt giảm giá SP
    private BigDecimal tongTien; // Tiền sau giảm SP
    private BigDecimal tienGiamGiaPhieu; // Tiền giảm do voucher
    private BigDecimal tongTienSauGiam; // Tiền sau khi áp voucher
    private BigDecimal phiVanChuyen; // Phí ship
    private BigDecimal thanhTien; // Tổng cuối cùng cần thanh toán
    private List<AdminBanHangHoaDonChiTietResponse> listsHoaDonChiTiet;
    private Boolean priceChanged;
    private String priceChangeMessage;
    
    // UI Suggestion fields for Voucher
    private String bestVoucherId;
    private String voucherSuggestionText;
    private String betterVoucherSuggestionText;
    private Boolean canApplySuggestedVoucher;
}
