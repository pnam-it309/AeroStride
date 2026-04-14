package com.example.be.core.admin.sanpham.service;

import com.example.be.core.admin.sanpham.model.request.CreateProductRequest;
import com.example.be.core.admin.sanpham.model.request.ProductVariantImageRequest;
import com.example.be.core.admin.sanpham.model.request.ProductVariantRequest;
import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.model.request.UpdateProductRequest;
import com.example.be.core.admin.sanpham.model.request.UpdateProductVariantImageRequest;
import com.example.be.core.admin.sanpham.model.response.ProductDetailResponse;
import com.example.be.core.admin.sanpham.model.response.ProductFormOptionsResponse;
import com.example.be.core.admin.sanpham.model.response.ProductResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantImageResponse;
import com.example.be.core.admin.sanpham.model.response.ProductVariantResponse;
import com.example.be.core.common.dto.PageResponse;
import java.util.List;

public interface AdminSanPhamService {

    List<ProductVariantResponse> getVariantsByProductId(String productId);

    ProductFormOptionsResponse getFormOptions();

    ProductDetailResponse createProduct(CreateProductRequest request);

    PageResponse<ProductResponse> getProducts(SearchProductRequest request);

    ProductDetailResponse getProductDetail(String id);

    ProductDetailResponse updateProduct(String id, UpdateProductRequest request);

    void deleteProduct(String id);

    void updateStatus(String id, com.example.be.infrastructure.constants.TrangThai status);

    ProductVariantResponse addVariant(String productId, ProductVariantRequest request);

    ProductVariantResponse updateVariant(String variantId, ProductVariantRequest request);

    void deleteVariant(String variantId);

    ProductVariantImageResponse addVariantImage(String variantId, ProductVariantImageRequest request);

    ProductVariantImageResponse updateVariantImage(String imageId, UpdateProductVariantImageRequest request);

    void deleteVariantImage(String imageId);

    byte[] exportExcel();

    byte[] downloadTemplate();

    void importExcel(org.springframework.web.multipart.MultipartFile file);
}
