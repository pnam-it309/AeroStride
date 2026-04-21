package com.example.be.core.admin.sanpham.service;

import com.example.be.BaseServiceTest;
import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.model.request.SearchProductRequest;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.core.admin.sanpham.repository.AdminSanPhamRepository;
import com.example.be.core.admin.sanpham.service.impl.AdminSanPhamServiceImpl;
import com.example.be.entity.SanPham;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        assertThat(result.getData()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}
