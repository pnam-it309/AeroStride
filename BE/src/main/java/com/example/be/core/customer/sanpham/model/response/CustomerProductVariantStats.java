package com.example.be.core.customer.sanpham.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProductVariantStats {
    private String sanPhamId;
    private Long tongBienThe;
    private Long tongSoLuongTon;
    private BigDecimal giaBanThapNhat;
    private BigDecimal giaBanCaoNhat;
}
