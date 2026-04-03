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
    private BigDecimal tongTien;
    private BigDecimal tongTienSauGiam;
    private List<AdminBanHangHoaDonChiTietResponse> listsHoaDonChiTiet;
}
