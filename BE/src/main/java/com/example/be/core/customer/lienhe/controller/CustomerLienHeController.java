package com.example.be.core.customer.lienhe.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.lienhe.model.request.CustomerLienHeRequest;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.CUSTOMER_LIEN_HE)
@RequiredArgsConstructor
public class CustomerLienHeController {

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitContact(@Valid @RequestBody CustomerLienHeRequest request) {
        log.info("Khách hàng gửi yêu cầu liên hệ: Tên={}, SĐT={}, Chủ đề={}", request.getTen(), request.getSdt(), request.getChuDe());
        return ResponseEntity.ok(ApiResponse.success(null, "Cảm ơn bạn! Yêu cầu hỗ trợ đã được gửi thành công. AeroStride sẽ sớm phản hồi."));
    }
}
