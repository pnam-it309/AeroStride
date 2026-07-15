package com.example.be.core.admin.banhang.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBanHangPaymentStatusResponse {
    private Boolean isPaid;
    private String transactionNo;
}
