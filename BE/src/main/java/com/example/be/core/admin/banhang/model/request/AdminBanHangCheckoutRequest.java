package com.example.be.core.admin.banhang.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class AdminBanHangCheckoutRequest {
    private String idKhachHang;
    private String idPhieuGiamGia;
    private BigDecimal tongTien;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTienSauGiam;
    private String loaiDon; // TAI_QUAY, GIAO_HANG
    private String ghiChu;

    // Mixed Payment Info
    private BigDecimal tienMat;
    private BigDecimal tienChuyenKhoan;
    private String maGiaoDich; // Mã GD chuyển khoản (vnp_TransactionNo or BANK_TX_ID)
}
