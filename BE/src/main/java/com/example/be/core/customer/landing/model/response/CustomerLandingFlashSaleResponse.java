package com.example.be.core.customer.landing.model.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLandingFlashSaleResponse {
    private String id;
    private String ma;
    private String ten;
    private String loaiGiamGia;
    private BigDecimal soTienGiam;
    private Long ngayBatDau;
    private Long ngayKetThuc;
    private String khungGio;
    private Boolean isHappening;
    private Long remainingMillis;
    private List<CustomerLandingFlashSaleItemResponse> items;
}
