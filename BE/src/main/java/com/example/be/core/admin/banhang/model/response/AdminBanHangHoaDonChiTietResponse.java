package com.example.be.core.admin.banhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminBanHangHoaDonChiTietResponse {
    private String id;
    private String idChiTietSanPham;
    private String tenSanPham;
    private String maChiTietSanPham;
    private String tenMauSac;
    private String tenKichThuoc;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal giaGoc;
    private Integer phanTramGiam;
    private String tenDotGiamGia;
    private BigDecimal thanhTien;
    private Integer soLuongTon;
    private String hinhAnh;
}
