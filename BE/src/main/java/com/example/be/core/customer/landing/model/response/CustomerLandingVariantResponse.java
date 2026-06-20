package com.example.be.core.customer.landing.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLandingVariantResponse {
    private String id;
    private String idSanPham;
    private String maSanPham;
    private String tenSanPham;
    private String tenThuongHieu;
    private String maChiTietSanPham;
    private String tenMauSac;
    private String maMauHex;
    private String tenKichThuoc;
    private String giaTriKichThuoc;
    private Integer soLuong;
    private BigDecimal giaBan;
    private BigDecimal phanTramGiam;
    private String hinhAnh;
    private List<String> images;
}
