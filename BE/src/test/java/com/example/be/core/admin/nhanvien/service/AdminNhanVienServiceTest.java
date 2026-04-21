package com.example.be.core.admin.nhanvien.service;

import com.example.be.BaseServiceTest;
import com.example.be.core.admin.nhanvien.model.request.AdminNhanVienRequest;
import com.example.be.core.admin.nhanvien.model.response.AdminNhanVienResponse;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.core.admin.nhanvien.service.impl.AdminNhanVienServiceImpl;
import com.example.be.entity.NhanVien;
import com.example.be.infrastructure.notification.EmailServiceImpl;
import com.example.be.utils.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AdminNhanVienServiceTest extends BaseServiceTest {

    @Mock
    private AdminNhanVienRepository adminNhanVienRepository;

    @Mock
    private EmailServiceImpl emailService;

    @InjectMocks
    private AdminNhanVienServiceImpl adminNhanVienService;

    @Test
    @DisplayName("Should return paginated employees when searching")
    void search_Success() {
        // Given (Arrange)
        AdminNhanVienRequest request = new AdminNhanVienRequest();
        request.setPage(0);
        request.setSize(10);
        request.setKeyword("Employee");

        AdminNhanVienResponse mockResponse = new AdminNhanVienResponse();
        mockResponse.setMa("NV-01");
        mockResponse.setTen("Employee 01");

        Page<AdminNhanVienResponse> mockPage = new PageImpl<>(List.of(mockResponse));

        when(adminNhanVienRepository.filterAll(any(), any(), any(), any())).thenReturn(mockPage);

        // When (Act)
        Page<AdminNhanVienResponse> result = adminNhanVienService.search(request);

        // Then (Assert)
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMa()).isEqualTo("NV-01");
    }
}
