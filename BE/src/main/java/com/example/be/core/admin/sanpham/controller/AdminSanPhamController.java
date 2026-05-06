package com.example.be.core.admin.sanpham.controller;

import java.util.List;
import java.util.Map;

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
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(RoutesConstant.ADMIN_SAN_PHAM)
@RequiredArgsConstructor
public class AdminSanPhamController {

    private final AdminSanPhamService adminSanPhamService;

    @GetMapping(RoutesConstant.FORM_OPTIONS)
    public ResponseEntity<ApiResponse<ProductFormOptionsResponse>> getFormOptions() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getFormOptions(), "Lay du lieu form san pham thanh cong"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.createProduct(request), "Tao san pham thanh cong"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(SearchProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getProducts(request), "Lay danh sach san pham thanh cong"));
    }

    @GetMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getProductDetail(id), "Lay chi tiet san pham thanh cong"));
    }

    @PutMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateProduct(id, request), "Cap nhat san pham thanh cong"));
    }

    @DeleteMapping(RoutesConstant.ID)
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        adminSanPhamService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa mem san pham thanh cong"));
    }

    @PutMapping(RoutesConstant.STATUS_ALT)
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        com.example.be.infrastructure.constants.TrangThai status = com.example.be.infrastructure.constants.TrangThai.valueOf(statusStr);
        adminSanPhamService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(null, "Cap nhat trang thai thanh cong"));
    }

    @GetMapping(RoutesConstant.VARIANTS_SUB)
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getVariantsByProductId(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getVariantsByProductId(id), "Lay danh sach bien the thanh cong"));
    }

    @GetMapping(RoutesConstant.VARIANTS)
    public ResponseEntity<ApiResponse<List<ProductVariantResponse>>> getAllVariants() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getAllVariants(), "Lay tat ca danh sach bien the thanh cong"));
    }

    @PostMapping(RoutesConstant.VARIANTS_SUB)
    public ResponseEntity<ApiResponse<ProductVariantResponse>> addVariant(
            @PathVariable String id,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.addVariant(id, request), "Them bien the thanh cong"));
    }

    @PutMapping(RoutesConstant.VARIANT_ID)
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable String variantId,
            @Valid @RequestBody ProductVariantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateVariant(variantId, request), "Cap nhat bien the thanh cong"));
    }

    @DeleteMapping(RoutesConstant.VARIANT_ID)
    public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable String variantId) {
        adminSanPhamService.deleteVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa mem bien the thanh cong"));
    }

    @PostMapping(RoutesConstant.VARIANT_IMAGES)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> addVariantImage(
            @PathVariable String variantId,
            @Valid @RequestBody ProductVariantImageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.addVariantImage(variantId, request), "Them anh bien the thanh cong"));
    }

    @PutMapping(RoutesConstant.VARIANT_IMAGE_ID)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> updateVariantImage(
            @PathVariable String imageId,
            @Valid @RequestBody UpdateProductVariantImageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.updateVariantImage(imageId, request), "Cap nhat anh bien the thanh cong"));
    }

    @PutMapping(RoutesConstant.VARIANT_IMAGE_MAIN)
    public ResponseEntity<ApiResponse<ProductVariantImageResponse>> setMainVariantImage(@PathVariable String imageId) {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.setMainVariantImage(imageId), "Dat anh chinh thanh cong"));
    }

    @DeleteMapping(RoutesConstant.VARIANT_IMAGE_ID)
    public ResponseEntity<ApiResponse<Void>> deleteVariantImage(@PathVariable String imageId) {
        adminSanPhamService.deleteVariantImage(imageId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoa mem anh bien the thanh cong"));
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
    public ResponseEntity<String> importExcel(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        adminSanPhamService.importExcel(file);
        return ResponseEntity.ok("Import thành công");
    }

    @GetMapping("/max-price")
    public ResponseEntity<ApiResponse<java.math.BigDecimal>> getMaxPrice() {
        return ResponseEntity.ok(ApiResponse.success(adminSanPhamService.getMaxPrice(), "Lay gia cao nhat thanh cong"));
    }
}
