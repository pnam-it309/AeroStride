package com.example.be.core.customer.voucher.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.voucher.model.response.CustomerVoucherResponse;
import com.example.be.core.customer.voucher.service.CustomerVoucherService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER + "/phieu-giam-gia")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final CustomerVoucherService voucherService;

    @GetMapping("/hien-thi")
    public ResponseEntity<ApiResponse<List<CustomerVoucherResponse>>> getPublicVouchers() {
        List<CustomerVoucherResponse> response = voucherService.getPublicVouchers();
        return ResponseEntity.ok(ApiResponse.success(response, "Lay danh sach phieu giam gia thanh cong"));
    }

    @GetMapping("/best-suggestion")
    public ResponseEntity<ApiResponse<CustomerVoucherResponse>> getBestSuggestion(
            @RequestParam("orderValue") BigDecimal orderValue) {
        CustomerVoucherResponse response = voucherService.getBestVoucherSuggestion(orderValue);
        return ResponseEntity.ok(ApiResponse.success(response, "Gợi ý voucher tốt nhất"));
    }
}
