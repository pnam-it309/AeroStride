package com.example.be.core.admin.banhang.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AdminBanHangCheckoutRequest {
    private String idKhachHang;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String emailKhachHang;
    private Boolean gioiTinhKhachHang;
    private LocalDate ngaySinhKhachHang;
    private String idPhieuGiamGia;
    private BigDecimal tongTien;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTienSauGiam;
    private String loaiDon; // TAI_QUAY, GIAO_HANG
    private String ghiChu;
    private String tenNguoiNhan;
    private String sdtNguoiNhan;
    private String diaChiNguoiNhan;
    private String tinh;
    private String thanhPho;
    private String phuongXa;
    private String diaChiChiTiet;
    private Boolean luuDiaChiMacDinh;

    // Mixed Payment Info
    private BigDecimal tienMat;
    private BigDecimal tienChuyenKhoan;
    private String maGiaoDich; // Mã GD chuyển khoản (vnp_TransactionNo or BANK_TX_ID)
}
