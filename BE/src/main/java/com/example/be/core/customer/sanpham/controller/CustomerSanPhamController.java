package com.example.be.core.customer.sanpham.controller;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER + "/san-pham")
@RequiredArgsConstructor
public class CustomerSanPhamController {

    private final AdminSanPhamService adminSanPhamService;

    @GetMapping("/hien-thi")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(SearchProductRequest request) {
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(12);
        
        PageResponse<ProductResponse> response = adminSanPhamService.getProducts(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lay danh sach san pham thanh cong"));
    }
}
