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

/**
 * Module: Sản phẩm (Customer)
 * Chức năng: Quản lý API phục vụ việc hiển thị danh sách sản phẩm, chi tiết sản phẩm,
 * và các bộ lọc tìm kiếm cho khách hàng trên cửa hàng trực tuyến.
 */
@RestController
@RequestMapping(RoutesConstant.CUSTOMER_SAN_PHAM)
@RequiredArgsConstructor
public class CustomerSanPhamController {

    private final CustomerSanPhamService customerSanPhamService;

    // Lấy danh sách sản phẩm đang hoạt động để hiển thị lên cửa hàng (hỗ trợ phân trang, tìm kiếm)
    @GetMapping("/hien-thi")
    public ResponseEntity<ApiResponse<PageResponse<CustomerProductResponse>>> getProducts(CustomerSearchProductRequest request) {
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(12);
        
        PageResponse<CustomerProductResponse> response = customerSanPhamService.getProducts(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Lay danh sach san pham thanh cong"));
    }

    // Lấy danh sách các bộ lọc (danh mục, thương hiệu, màu sắc, kích thước...) để tìm kiếm sản phẩm
    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<CustomerProductFormOptionsResponse>> getFilters() {
        return ResponseEntity.ok(ApiResponse.success(customerSanPhamService.getFormOptions(), "Lay danh sach bo loc thanh cong"));
    }

    // Lấy thông tin chi tiết một sản phẩm (bao gồm hình ảnh, các phiên bản màu sắc, kích thước)
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<CustomerProductDetailResponse>> getProductDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(customerSanPhamService.getProductDetail(id), "Lay chi tiet san pham thanh cong"));
    }
}

