package com.example.be.core.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    private BigDecimal amount;
    private String orderId;
    private String orderInfo;
}
