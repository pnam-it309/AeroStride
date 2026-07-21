package com.example.be.core.customer.cart.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.cart.model.request.CustomerCartSyncRequest;
import com.example.be.core.customer.cart.model.response.CustomerCartSyncResponse;
import com.example.be.core.customer.cart.service.CustomerCartService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER_CART)
@RequiredArgsConstructor
public class CustomerCartController {

    private final CustomerCartService cartService;

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<CustomerCartSyncResponse>> syncCart(@RequestBody CustomerCartSyncRequest request) {
        CustomerCartSyncResponse response = cartService.syncCart(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đồng bộ giỏ hàng thành công"));
    }
}
