package com.example.be.core.admin.banhang.model.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class BanHangSanPhamResponse {
    private String id;
    private String tenSanPham;
    private String maChiTietSanPham;
    private String maSanPham;
    private String tenDanhMuc;
    private String tenThuongHieu;
    private String tenChatLieu;
    private String tenDeGiay;
    private String tenMauSac;
    private String tenKichThuoc;
    private Integer soLuongTon;
    private BigDecimal giaBan;
    private String hinhAnh;
}
