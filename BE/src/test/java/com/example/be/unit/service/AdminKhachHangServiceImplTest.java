package com.example.be.core.admin.khachhang.service.impl;

import com.example.be.core.admin.khachhang.model.request.AdminKhachHangRequest;
import com.example.be.core.admin.khachhang.model.response.AdminKhachHangResponse;
import com.example.be.core.admin.khachhang.repository.AdminKhachHangRepository;
import com.example.be.core.admin.khachhang.service.AdminDiaChiService;
import com.example.be.repository.DiaChiRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminKhachHangServiceImplTest {

    @Mock
    private AdminKhachHangRepository adminKhachHangRepository;

    @Mock
    private DiaChiRepository diaChiRepository;

    @Mock
    private AdminDiaChiService adminDiaChiService;

    @Mock
    private com.example.be.core.notification.EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

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
        when(adminKhachHangRepository.existsByMa("KH001")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.add(testRequest));
        assertEquals("Mã khách hàng này đã tồn tại trong hệ thống.", exception.getMessage());
    }

    @Test
    void add_ShouldThrowException_WhenEmailExists() {
        when(adminKhachHangRepository.existsByMa(any())).thenReturn(false);
        when(adminKhachHangRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.add(testRequest));
        assertEquals("Email này đã được sử dụng bởi một khách hàng khác.", exception.getMessage());
    }


    @Test
    void update_ShouldThrowException_WhenUserNotFound() {
        when(adminKhachHangRepository.findById("999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.update("999", testRequest));
    }

    @Test
    void add_ShouldSaveSuccess() {
        when(adminKhachHangRepository.existsByMa(any())).thenReturn(false);
        when(adminKhachHangRepository.findAllMa()).thenReturn(java.util.Collections.emptyList());
        when(adminKhachHangRepository.existsByEmail(any())).thenReturn(false);
        when(adminKhachHangRepository.existsByTenTaiKhoan(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        
        AdminKhachHangResponse mockResponse = new AdminKhachHangResponse();
        when(adminKhachHangRepository.detail(any())).thenReturn(mockResponse);
        when(adminDiaChiService.getByKhachHangId(any())).thenReturn(java.util.Collections.emptyList());

        var result = service.add(testRequest);

        assertNotNull(result);
        verify(adminKhachHangRepository).save(any(KhachHang.class));
        verify(passwordEncoder).encode("rawPassword");
    }

    @Test
    void delete_ShouldUpdateStatus() {
        when(adminKhachHangRepository.findById("1")).thenReturn(Optional.of(testEntity));

        service.delete("1");

        assertEquals(TrangThai.KHONG_HOAT_DONG, testEntity.getTrangThai());
        verify(adminKhachHangRepository).save(testEntity);
    }
}
