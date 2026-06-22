package com.example.be.core.customer.landing.controller;

import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingProductResponse;
import com.example.be.core.customer.landing.model.response.CustomerLandingVariantResponse;
import com.example.be.core.customer.landing.service.CustomerLandingService;
import com.example.be.infrastructure.config.ratelimit.RateLimit;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER + "/landing")
@RequiredArgsConstructor
public class CustomerLandingController {

    private final CustomerLandingService landingService;

    @GetMapping("/products")
    @RateLimit(limit = 60, windowSeconds = 60)
    public ResponseEntity<ApiResponse<List<CustomerLandingProductResponse>>> getLandingProducts(
            @RequestParam(defaultValue = "6") Integer size
    ) {
        List<CustomerLandingProductResponse> response = landingService.getLandingProducts(size);
        return ResponseEntity.ok(ApiResponse.success(response, "Lay san pham landing thanh cong"));
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<List<List<com.example.be.core.customer.landing.model.response.CustomerLandingFeatureItemResponse>>>> getLandingFeatures() {
        return ResponseEntity.ok(ApiResponse.success(landingService.getLandingFeatures(), "Lay danh sach tinh nang landing thanh cong"));
    }

    @GetMapping("/featured-variants")
    @RateLimit(limit = 60, windowSeconds = 60)
    public ResponseEntity<ApiResponse<List<CustomerLandingVariantResponse>>> getFeaturedVariants(
            @RequestParam(defaultValue = "12") Integer size
    ) {
        List<CustomerLandingVariantResponse> response = landingService.getFeaturedVariants(size);
        return ResponseEntity.ok(ApiResponse.success(response, "Lay bien the noi bat thanh cong"));
    }
}
