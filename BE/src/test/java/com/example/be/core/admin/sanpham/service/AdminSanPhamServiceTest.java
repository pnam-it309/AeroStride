package com.example.be.core.admin.sanpham.service;

import com.example.be.BaseServiceTest;
import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.ProductVariantRequest;
import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.core.admin.sanpham.service.impl.AdminSanPhamServiceImpl;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.utils.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSanPhamServiceTest extends BaseServiceTest {

    @Mock
    private AdminSanPhamRepository adminSanPhamRepository;

    @Mock
    private AdminChiTietSanPhamRepository adminChiTietSanPhamRepository;

    @Mock
    private AdminSanPhamMapper adminSanPhamMapper;

    @InjectMocks
    private AdminSanPhamServiceImpl adminSanPhamService;

    @Test
    @DisplayName("Unit Test: Should return empty page when no product matches keyword")
    void getProducts_NoMatch() {
        // Arrange
        SearchProductRequest request = new SearchProductRequest();
        request.setKeyword("NonExistingProduct");
        request.setPage(0);
        request.setSize(10);

        when(adminSanPhamRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        var result = adminSanPhamService.getProducts(request);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Unit Test: Should throw DuplicateResourceException when adding a duplicate variant")
    void addVariant_DuplicateThrowsException() {
        // Arrange
        String productId = "product-123";
        SanPham sanPham = new SanPham();
        sanPham.setId(productId);

        ProductVariantRequest request = new ProductVariantRequest();
        request.setIdMauSac("ms-1");
        request.setIdKichThuoc("kt-1");

        when(adminSanPhamRepository.findByIdAndXoaMemFalse(productId)).thenReturn(Optional.of(sanPham));
        when(adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalse(
                productId, request.getIdMauSac(), request.getIdKichThuoc())).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            adminSanPhamService.addVariant(productId, request);
        });

        assertThat(exception.getMessage()).isEqualTo(MessageConstants.SAN_PHAM_VARIANT_DUPLICATE);
        verify(adminChiTietSanPhamRepository, never()).save(any(ChiTietSanPham.class));
    }

    @Test
    @DisplayName("Unit Test: Should throw DuplicateResourceException when updating to a duplicate variant")
    void updateVariant_DuplicateThrowsException() {
        // Arrange
        String variantId = "variant-123";
        String productId = "product-123";

        SanPham sanPham = new SanPham();
        sanPham.setId(productId);

        ChiTietSanPham existingVariant = new ChiTietSanPham();
        existingVariant.setId(variantId);
        existingVariant.setSanPham(sanPham);

        ProductVariantRequest request = new ProductVariantRequest();
        request.setIdMauSac("ms-2");
        request.setIdKichThuoc("kt-2");

        when(adminChiTietSanPhamRepository.findByIdAndXoaMemFalse(variantId)).thenReturn(Optional.of(existingVariant));
        when(adminChiTietSanPhamRepository.existsBySanPhamIdAndMauSacIdAndKichThuocIdAndXoaMemFalseAndIdNot(
                productId, request.getIdMauSac(), request.getIdKichThuoc(), variantId)).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(DuplicateResourceException.class, () -> {
            adminSanPhamService.updateVariant(variantId, request);
        });

        assertThat(exception.getMessage()).isEqualTo(MessageConstants.SAN_PHAM_VARIANT_DUPLICATE);
        verify(adminChiTietSanPhamRepository, never()).save(any(ChiTietSanPham.class));
    }
}

