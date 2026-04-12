package com.example.be.core.admin.thuoctinh.thuonghieu.service.impl;
import com.example.be.core.admin.thuoctinh.thuonghieu.repository.AdminThuongHieuRepository;
import com.example.be.entity.ThuongHieu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminThuongHieuServiceImplTest {
    @Mock private AdminThuongHieuRepository repository;
    @InjectMocks private AdminThuongHieuServiceImpl service;
    @Test void testFindById() {
        ThuongHieu entity = new ThuongHieu();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertTrue(service.findById(1L).isPresent());
    }
}
