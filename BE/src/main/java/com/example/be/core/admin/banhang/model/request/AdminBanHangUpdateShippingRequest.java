package com.example.be.core.admin.banhang.model.request;

import com.example.be.infrastructure.constants.DeliveryMethod;
import com.example.be.infrastructure.constants.OrderType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminBanHangUpdateShippingRequest {
    private OrderType orderType;
    private DeliveryMethod deliveryMethod;
    @Deprecated
    private String loaiDon;
    private BigDecimal phiVanChuyen;
}
