package com.example.be.core.customer.sanpham.service;

import com.example.be.core.common.dto.PageResponse;
import com.example.be.core.customer.sanpham.model.request.CustomerSearchProductRequest;
import com.example.be.core.customer.sanpham.model.response.CustomerProductDetailResponse;
import com.example.be.core.customer.sanpham.model.response.CustomerProductFormOptionsResponse;
import com.example.be.core.customer.sanpham.model.response.CustomerProductResponse;

public interface CustomerSanPhamService {
    PageResponse<CustomerProductResponse> getProducts(CustomerSearchProductRequest request);
    CustomerProductFormOptionsResponse getFormOptions();
    CustomerProductDetailResponse getProductDetail(String id);
}
