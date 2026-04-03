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
    private String tenMauSac;
    private String tenKichThuoc;
    private Integer soLuongTon;
    private BigDecimal giaBan;
}
