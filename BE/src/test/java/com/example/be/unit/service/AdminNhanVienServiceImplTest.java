package com.example.be.core.admin.nhanvien.service.impl;
import com.example.be.core.admin.nhanvien.repository.AdminNhanVienRepository;
import com.example.be.entity.NhanVien;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminNhanVienServiceImplTest {
    @Mock private AdminNhanVienRepository repository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AdminNhanVienServiceImpl service;
    @Test void testFindById() {
        NhanVien entity = new NhanVien();
        when(repository.findById("1")).thenReturn(Optional.of(entity));
        assertTrue(service.findById("1").isPresent());
    }
}
