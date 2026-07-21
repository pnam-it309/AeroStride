package com.example.be.core.admin.sanpham.service;

import com.example.be.BaseServiceTest;
import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.CreateProductRequest;
import com.example.be.core.admin.sanpham.model.request.ProductVariantRequest;
import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.model.request.UpdateProductRequest;
import com.example.be.core.admin.sanpham.model.response.ProductDetailResponse;
import com.example.be.core.admin.sanpham.repository.AdminAnhChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.core.admin.sanpham.service.impl.AdminSanPhamServiceImpl;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.DuplicateResourceException;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import com.example.be.repository.*;
import com.example.be.utils.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdminSanPhamServiceTest extends BaseServiceTest {

    @Mock
    private AdminSanPhamRepository adminSanPhamRepository;

    @Mock
    private AdminChiTietSanPhamRepository adminChiTietSanPhamRepository;

    @Mock
    private AdminAnhChiTietSanPhamRepository adminAnhChiTietSanPhamRepository;

    @Mock
    private AdminSanPhamMapper adminSanPhamMapper;

    @Mock
    private ThuongHieuRepository thuongHieuRepository;

    @Mock
    private XuatXuRepository xuatXuRepository;

    @Mock
    private MucDichChayRepository mucDichChayRepository;

    @Mock
    private CoGiayRepository coGiayRepository;

    @Mock
    private ChatLieuRepository chatLieuRepository;

    @Mock
    private DeGiayRepository deGiayRepository;

    @Mock
    private MauSacRepository mauSacRepository;

    @Mock
    private KichThuocRepository kichThuocRepository;

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
    @DisplayName("Unit Test: Should create product successfully")
    void createProduct_Success() {
        // Arrange
        CreateProductRequest request = new CreateProductRequest();
        request.setTenSanPham("New Shoes");
        request.setIdThuongHieu("th-1");
        request.setIdXuatXu("xx-1");
        request.setIdMucDichChay("md-1");
        request.setIdCoGiay("cg-1");
        request.setIdChatLieu("cl-1");
        request.setIdDeGiay("dg-1");
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);

        when(adminSanPhamRepository.existsByMaIgnoreCaseAndXoaMemFalse(anyString())).thenReturn(false);
        when(adminSanPhamRepository.save(any(SanPham.class))).thenAnswer(invocation -> {
            SanPham sp = invocation.getArgument(0);
            sp.setId("new-id");
            return sp;
        });
        
        // Mock finding the product for buildProductDetailResponse
        SanPham mockSaved = TestDataFactory.createMockSanPham();
        when(adminSanPhamRepository.findByIdAndXoaMemFalse("new-id")).thenReturn(Optional.of(mockSaved));
        when(adminSanPhamMapper.toProductDetailResponse(any(), any())).thenReturn(new ProductDetailResponse());

        // Act
        var result = adminSanPhamService.createProduct(request);

        // Assert
        assertThat(result).isNotNull();
        verify(adminSanPhamRepository).save(any(SanPham.class));
    }

    @Test
    @DisplayName("Unit Test: Should update product successfully")
    void updateProduct_Success() {
        // Arrange
        String productId = "product-1";
        UpdateProductRequest request = new UpdateProductRequest();
        request.setTenSanPham("Updated Name");
        request.setTrangThai(TrangThai.DANG_HOAT_DONG);

        SanPham existingProduct = TestDataFactory.createMockSanPham();
        existingProduct.setId(productId);

        when(adminSanPhamRepository.findByIdAndXoaMemFalse(productId)).thenReturn(Optional.of(existingProduct));
        when(adminSanPhamRepository.save(any(SanPham.class))).thenReturn(existingProduct);
        when(adminSanPhamMapper.toProductDetailResponse(any(), any())).thenReturn(new ProductDetailResponse());

        // Act
        var result = adminSanPhamService.updateProduct(productId, request);

        // Assert
        assertThat(result).isNotNull();
        verify(adminSanPhamRepository).save(existingProduct);
        assertThat(existingProduct.getTen()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Unit Test: Should soft delete product and its variants")
    void deleteProduct_Success() {
        // Arrange
        String productId = "product-1";
        SanPham existingProduct = TestDataFactory.createMockSanPham();
        existingProduct.setId(productId);

        ChiTietSanPham variant = TestDataFactory.createMockVariant(existingProduct);
        variant.setId("variant-1");

        when(adminSanPhamRepository.findByIdAndXoaMemFalse(productId)).thenReturn(Optional.of(existingProduct));
        when(adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(productId)).thenReturn(List.of(variant));

        // Act
        adminSanPhamService.deleteProduct(productId);

        // Assert
        assertThat(existingProduct.getXoaMem()).isTrue();
        assertThat(existingProduct.getTrangThai()).isEqualTo(TrangThai.DA_XOA);
        assertThat(variant.getXoaMem()).isTrue();
        assertThat(variant.getTrangThai()).isEqualTo(TrangThai.DA_XOA);
        
        verify(adminSanPhamRepository).save(existingProduct);
        verify(adminChiTietSanPhamRepository).save(variant);
    }

    @Test
    @DisplayName("Unit Test: Should update status for product and its variants")
    void updateStatus_Success() {
        // Arrange
        String productId = "product-1";
        SanPham existingProduct = TestDataFactory.createMockSanPham();
        existingProduct.setId(productId);
        existingProduct.setTrangThai(TrangThai.DANG_HOAT_DONG);

        ChiTietSanPham variant = TestDataFactory.createMockVariant(existingProduct);
        variant.setTrangThai(TrangThai.DANG_HOAT_DONG);

        when(adminSanPhamRepository.findByIdAndXoaMemFalse(productId)).thenReturn(Optional.of(existingProduct));
        when(adminChiTietSanPhamRepository.findBySanPhamIdAndXoaMemFalse(productId)).thenReturn(List.of(variant));

        // Act
        adminSanPhamService.updateStatus(productId, TrangThai.NGUNG_HOAT_DONG);

        // Assert
        assertThat(existingProduct.getTrangThai()).isEqualTo(TrangThai.NGUNG_HOAT_DONG);
        assertThat(variant.getTrangThai()).isEqualTo(TrangThai.NGUNG_HOAT_DONG);
        
        verify(adminSanPhamRepository).save(existingProduct);
        verify(adminChiTietSanPhamRepository).save(variant);
    }

    @Test
    @DisplayName("Unit Test: Should throw ResourceNotFoundException when product does not exist")
    void getProductDetail_NotFound() {
        // Arrange
        String productId = "invalid-id";
        when(adminSanPhamRepository.findByIdAndXoaMemFalse(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            adminSanPhamService.getProductDetail(productId);
        });
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
