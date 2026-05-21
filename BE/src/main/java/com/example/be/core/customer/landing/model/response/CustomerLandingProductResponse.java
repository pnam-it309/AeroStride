package com.example.be.core.customer.landing.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLandingProductResponse {
    private String id;
    private String maSanPham;
    private String tenSanPham;
    private String tenDanhMuc;
    private String tenThuongHieu;
    private String hinhAnh;
    private BigDecimal giaBanThapNhat;
    private BigDecimal giaBanCaoNhat;
}
