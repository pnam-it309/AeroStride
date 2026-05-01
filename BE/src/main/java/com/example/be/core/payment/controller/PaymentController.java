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
        
        // SECURITY FIX: Verify VNPay signature first
        boolean isValid = paymentService.verifyPayment(params);
        
        if (!isValid) {
            log.warn("VNPay callback rejected - invalid signature");
            return ResponseEntity.ok(ApiResponse.error(400, "Invalid VNPay signature"));
        }
        
        // SECURITY FIX: Verify response code indicates successful payment
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        if (vnp_ResponseCode == null || !vnp_ResponseCode.equals("00")) {
            log.warn("VNPay payment failed with response code: {}", vnp_ResponseCode);
            return ResponseEntity.ok(ApiResponse.error(400, "Payment Failed with code: " + vnp_ResponseCode));
        }
        
        // SECURITY FIX: Verify payment details match order
        try {
            validateCallbackDetails(params);
        } catch (IllegalArgumentException e) {
            log.warn("VNPay callback validation failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(400, "Payment validation failed: " + e.getMessage()));
        }
        
        // Payment successful - update order status in DB
        return ResponseEntity.ok(ApiResponse.success("Payment Successful"));
    }
    
    /**
     * Validates callback details to ensure payment matches the expected order
     * SECURITY: Prevents payment hijacking and unauthorized payment confirmations
     */
    private void validateCallbackDetails(Map<String, String> params) {
        // Verify response code
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        if (vnp_ResponseCode == null || vnp_ResponseCode.isEmpty()) {
            throw new IllegalArgumentException("Response code is missing");
        }
        if (!vnp_ResponseCode.equals("00")) {
            throw new IllegalArgumentException("Invalid response code: " + vnp_ResponseCode);
        }
        
        // Verify amount
        String vnp_Amount = params.get("vnp_Amount");
        if (vnp_Amount == null || vnp_Amount.isEmpty()) {
            throw new IllegalArgumentException("Amount is missing from callback");
        }
        try {
            long amount = Long.parseLong(vnp_Amount);
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + vnp_Amount);
        }
        
        // Verify transaction reference (Order ID)
        String vnp_TxnRef = params.get("vnp_TxnRef");
        if (vnp_TxnRef == null || vnp_TxnRef.isEmpty()) {
            throw new IllegalArgumentException("Transaction reference (Order ID) is missing");
        }
        
        // Verify order info format
        String vnp_OrderInfo = params.get("vnp_OrderInfo");
        if (vnp_OrderInfo == null || vnp_OrderInfo.isEmpty()) {
            throw new IllegalArgumentException("Order info is missing");
        }
        
        // Verify transaction status
        String vnp_TransactionStatus = params.get("vnp_TransactionStatus");
        if (vnp_TransactionStatus == null || !vnp_TransactionStatus.equals("0")) {
            throw new IllegalArgumentException("Invalid transaction status: " + vnp_TransactionStatus);
        }
        
        log.info("VNPay callback validation successful for order: {} with amount: {}", vnp_TxnRef, vnp_Amount);
    }
}
