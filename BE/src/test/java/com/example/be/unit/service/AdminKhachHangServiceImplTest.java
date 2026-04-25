package com.example.be.unit.service;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.impl.AdminKhachHangServiceImpl;
import com.example.be.entity.KhachHang;
import com.example.be.infrastructure.constants.TrangThai;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.core.notification.EmailService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminKhachHangServiceImplTest {

    @Mock
    private AdminKhachHangRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminDiaChiService adminDiaChiService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminKhachHangServiceImpl service;

    private AdminKhachHangRequest testRequest;
    private KhachHang testEntity;

    @BeforeEach
    void setUp() {
        testRequest = new AdminKhachHangRequest();
        testRequest.setMa("KH001");
        testRequest.setEmail("test@gmail.com");
        testRequest.setTenTaiKhoan("testuser");
        testRequest.setMatKhau("rawPassword");

        testEntity = new KhachHang();
        testEntity.setId("1");
        testEntity.setMa("KH001");
        testEntity.setEmail("test@gmail.com");
    }

    @Test
    void add_ShouldThrowException_WhenMaExists() {
        when(repository.existsByMa("KH001")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.add(testRequest));
        assertEquals("Mã khách hàng này đã tồn tại trong hệ thống.", exception.getMessage());
    }

    @Test
    void add_ShouldThrowException_WhenEmailExists() {
        when(repository.existsByMa(any())).thenReturn(false);
        when(repository.existsByEmail("test@gmail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.add(testRequest));
        assertEquals("Email này đã được sử dụng bởi một khách hàng khác.", exception.getMessage());
    }

    @Test
    void update_ShouldThrowException_WhenUserNotFound() {
        when(repository.findById("999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.update("999", testRequest));
    }

    @Test
    void add_ShouldSaveSuccess() {
        when(repository.existsByMa(any())).thenReturn(false);
        when(repository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        AdminKhachHangResponse mockResponse = mock(AdminKhachHangResponse.class);
        when(repository.detail(any())).thenReturn(mockResponse);

        var result = service.add(testRequest);

        assertNotNull(result);
        verify(repository).save(any(KhachHang.class));
    }

    @Test
    void delete_ShouldUpdateStatus() {
        when(repository.findById("1")).thenReturn(Optional.of(testEntity));

        service.delete("1");

        assertEquals(TrangThai.KHONG_HOAT_DONG, testEntity.getTrangThai());
        verify(repository).save(testEntity);
    }
}
