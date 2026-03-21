package com.example.be.core.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private String paymentUrl;
    private String transactionId;
    private String status;
}
