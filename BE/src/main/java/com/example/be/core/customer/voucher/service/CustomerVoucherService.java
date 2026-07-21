package com.example.be.core.customer.voucher.service;

import com.example.be.core.customer.voucher.model.response.CustomerVoucherResponse;
import java.util.List;
import java.math.BigDecimal;

public interface CustomerVoucherService {
    List<CustomerVoucherResponse> getPublicVouchers();
    CustomerVoucherResponse getBestVoucherSuggestion(BigDecimal orderValue);
}
