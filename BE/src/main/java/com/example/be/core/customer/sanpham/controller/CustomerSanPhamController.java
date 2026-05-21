package com.example.be.core.customer.sanpham.controller;

import com.example.be.core.customer.sanpham.model.request.CustomerSearchProductRequest;
import com.example.be.core.customer.sanpham.model.response.CustomerProductDetailResponse;
import com.example.be.core.customer.sanpham.model.response.CustomerProductFormOptionsResponse;
import com.example.be.core.customer.sanpham.model.response.CustomerProductResponse;
import com.example.be.core.customer.sanpham.service.CustomerSanPhamService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER + "/san-pham")
@RequiredArgsConstructor
public class CustomerSanPhamController {

    private final CustomerSanPhamService customerSanPhamService;

    @GetMapping("/hien-thi")
    public ResponseEntity<ApiResponse<PageResponse<CustomerProductResponse>>> getProducts(CustomerSearchProductRequest request) {
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(12);
        
        PageResponse<CustomerProductResponse> response = customerSanPhamService.getProducts(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lay danh sach san pham thanh cong"));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<CustomerProductFormOptionsResponse>> getFilters() {
        return ResponseEntity.ok(ApiResponse.success(customerSanPhamService.getFormOptions(), "Lay danh sach bo loc thanh cong"));
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<CustomerProductDetailResponse>> getProductDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(customerSanPhamService.getProductDetail(id), "Lay chi tiet san pham thanh cong"));
    }
}

