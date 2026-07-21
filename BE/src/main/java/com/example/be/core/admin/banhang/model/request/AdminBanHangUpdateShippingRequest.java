package com.example.be.core.admin.banhang.model.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminBanHangUpdateShippingRequest {
    private String loaiDon;
    private BigDecimal phiVanChuyen;
}
