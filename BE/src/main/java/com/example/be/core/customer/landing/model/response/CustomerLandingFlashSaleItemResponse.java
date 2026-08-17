package com.example.be.core.customer.landing.model.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLandingFlashSaleItemResponse {
    private String id;
    private String idSanPham;
    private String maSanPham;
    private String tenSanPham;
    private String tenThuongHieu;
    private String maChiTietSanPham;
    private String tenMauSac;
    private String maMauHex;
    private String tenKichThuoc;
    private Integer soLuong;
    private Integer daBan;
    private BigDecimal giaGoc;
    private BigDecimal giaFlashSale;
    private Integer phanTramGiam;
    private String hinhAnh;
}
