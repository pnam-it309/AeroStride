package com.example.be.core.payment.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.payment.PaymentService;
import com.example.be.core.payment.dto.PaymentRequest;
import com.example.be.core.payment.dto.PaymentResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.API_PREFIX + "/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@RequestBody PaymentRequest request) {
        String paymentUrl = paymentService.createPaymentUrl(request);
        PaymentResponse response = PaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .status("PENDING")
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<ApiResponse<String>> vnpayCallback(@RequestParam Map<String, String> params) {
        log.info("VNPay callback received metadata: {}", params);
        boolean isValid = paymentService.verifyPayment(params);
        
        if (isValid) {
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            if ("00".equals(vnp_ResponseCode)) {
                // Payment successful - update order status in DB
                return ResponseEntity.ok(ApiResponse.success("Payment Successful"));
            } else {
                return ResponseEntity.ok(ApiResponse.error(400, "Payment Failed with code: " + vnp_ResponseCode));
            }
        } else {
            return ResponseEntity.ok(ApiResponse.error(400, "Invalid Checksum Signature"));
        }
    }
}
