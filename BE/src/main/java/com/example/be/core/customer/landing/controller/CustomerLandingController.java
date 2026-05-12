package com.example.be.core.customer.landing.controller;

import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.model.response.ProductResponse;
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
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

    private final AdminSanPhamService adminSanPhamService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLandingProducts(
            @RequestParam(defaultValue = "6") Integer size
    ) {
        SearchProductRequest request = new SearchProductRequest();
        request.setPage(1);
        request.setSize(size);
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);

        PageResponse<ProductResponse> response = adminSanPhamService.getProducts(request);
        return ResponseEntity.ok(ApiResponse.success(response.getContent(), "Lay san pham landing thanh cong"));
    }
}
