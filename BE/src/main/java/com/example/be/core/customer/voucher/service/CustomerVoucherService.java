package com.example.be.core.customer.voucher.service;

import com.example.be.core.customer.voucher.model.response.CustomerVoucherResponse;
import java.util.List;

public interface CustomerVoucherService {
    List<CustomerVoucherResponse> getPublicVouchers();
}
