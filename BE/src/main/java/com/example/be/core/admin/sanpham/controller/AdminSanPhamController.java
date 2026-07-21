package com.example.be.core.admin.sanpham.controller;

import java.util.List;

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
import com.example.be.core.admin.sanpham.service.AdminSanPhamService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.core.common.dto.PageResponse;
import com.example.be.core.common.dto.UpdateStatusRequest;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.be.infrastructure.constants.VaiTro;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_SAN_PHAM)
@RequiredArgsConstructor
@PreAuthorize(VaiTro.PRE_AUTH_ADMIN_STAFF)
public class AdminSanPhamController {

    private final AdminSanPhamService adminSanPhamService;

    // Lấy các tùy chọn (form options) cần thiết khi tạo/sửa sản phẩm (thương hiệu, chất liệu, đế giày,...)
    @GetMapping(RoutesConstant.FORM_OPTIONS)
    public ResponseEntity<ApiResponse<ProductFormOptionsResponse>> getFormOptions() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getFormOptions(), MessageConstants.SAN_PHAM_FORM_OPTIONS_SUCCESS));
    }

    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> checkNameExist(@RequestParam String name) {
        java.util.Map<String, Object> data = adminSanPhamService.checkNameExist(name.trim());
        return ResponseEntity.ok(ApiResponse.success(data, MessageConstants.SAN_PHAM_CHECK_NAME_SUCCESS));
    }

    // Tạo mới một sản phẩm cùng với thông tin chi tiết
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.createProduct(request), MessageConstants.SAN_PHAM_CREATE_SUCCESS));
    }

    @PostMapping("/check-attributes")
    public ResponseEntity<ApiResponse<com.example.be.core.admin.sanpham.model.response.DuplicateAttributeResponse>> checkDuplicateAttributes(
            @Valid @RequestBody com.example.be.core.admin.sanpham.model.request.CheckDuplicateAttributesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.checkDuplicateAttributes(request), "Kiểm tra trùng thuộc tính thành công"));
    }

    // Lấy danh sách sản phẩm (có phân trang và tìm kiếm theo tiêu chí)
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(SearchProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getProducts(request), MessageConstants.SAN_PHAM_LIST_SUCCESS));
    }

    @GetMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getProductDetail(id), MessageConstants.SAN_PHAM_DETAIL_SUCCESS));
    }

    @PutMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateProduct(id, request), MessageConstants.SAN_PHAM_UPDATE_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        adminSanPhamService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.SAN_PHAM_DELETE_SUCCESS));
    }

    @PatchMapping(RoutesConstant.STATUS_ALT)
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest body
    ) {
        adminSanPhamService.updateStatus(id, body.getStatus());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.UPDATE_STATUS_SUCCESS));
    }

    // Lấy danh sách biến thể (màu sắc, kích thước, số lượng) của một sản phẩm
    @GetMapping(RoutesConstant.VARIANTS_SUB)
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProductId(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getVariantsByProductId(id), MessageConstants.VARIANT_LIST_SUCCESS));
    }

    @GetMapping(RoutesConstant.VARIANTS)
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getAllVariants() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getAllVariants(), MessageConstants.VARIANT_LIST_SUCCESS));
    }

    // Lấy danh sách biến thể có phân trang + lọc (server-side)
    @GetMapping(RoutesConstant.VARIANTS_SEARCH)
    public ResponseEntity<ApiResponse<com.example.be.core.common.dto.PageResponse<ProductVariantResponse>>> searchVariants(
            com.example.be.core.admin.sanpham.model.request.SearchVariantRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getVariantsPaged(request), MessageConstants.VARIANT_LIST_SUCCESS));
    }

    // Thêm mới một biến thể (chi tiết sản phẩm) cho sản phẩm hiện tại
    @PostMapping(RoutesConstant.VARIANTS_SUB)
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable String id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.addVariant(id, request), MessageConstants.VARIANT_ADD_SUCCESS));
    }

    // Cập nhật thông tin của một biến thể (số lượng, giá, trạng thái,...)
    @PutMapping(RoutesConstant.VARIANT_ID)
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable String variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateVariant(variantId, request), MessageConstants.VARIANT_UPDATE_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.VARIANT_ID)
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable String variantId) {
        adminSanPhamService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.VARIANT_DELETE_SUCCESS));
    }

    // Thêm hình ảnh mới cho một biến thể cụ thể
    @PostMapping(RoutesConstant.VARIANT_IMAGES)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> addVariantImage(
            @PathVariable String variantId,
            @Valid @RequestBody ProductVariantImageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.addVariantImage(variantId, request), MessageConstants.VARIANT_IMAGE_ADD_SUCCESS));
    }

    @PutMapping(RoutesConstant.VARIANT_IMAGE_ID)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> updateVariantImage(
            @PathVariable String imageId,
            @Valid @RequestBody UpdateProductVariantImageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateVariantImage(imageId, request), MessageConstants.VARIANT_IMAGE_UPDATE_SUCCESS));
    }

    // Thiết lập một hình ảnh thành ảnh đại diện (ảnh chính) cho biến thể
    @PutMapping(RoutesConstant.VARIANT_IMAGE_MAIN)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> setMainVariantImage(@PathVariable String imageId) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.setMainVariantImage(imageId), MessageConstants.VARIANT_IMAGE_SET_MAIN_SUCCESS));
    }

    @DeleteMapping(RoutesConstant.VARIANT_IMAGE_ID)
    public ResponseEntity<ApiResponse<Void>> deleteVariantImage(@PathVariable String imageId) {
        adminSanPhamService.deleteVariantImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.VARIANT_IMAGE_DELETE_SUCCESS));
    }

    @GetMapping(RoutesConstant.EXPORT_EXCEL)
    public ResponseEntity<byte[]> exportExcel() {
        byte[] excelContent = adminSanPhamService.exportExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_san_pham.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelContent);
    }

    @GetMapping(RoutesConstant.DOWNLOAD_TEMPLATE)
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] data = adminSanPhamService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=template_nhap_san_pham.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping(RoutesConstant.IMPORT_EXCEL)
    public ResponseEntity<ApiResponse<Void>> importExcel(@RequestParam("file") MultipartFile file) {
        adminSanPhamService.importExcel(file);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstants.IMPORT_SUCCESS));
    }

    @GetMapping(RoutesConstant.MAX_PRICE)
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> getMaxPrice() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getMaxPrice(), MessageConstants.SAN_PHAM_MAX_PRICE_SUCCESS));
    }
}
