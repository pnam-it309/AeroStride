package com.example.be.core.admin.thuoctinh.kichthuoc.service.impl;
import com.example.be.core.admin.thuoctinh.kichthuoc.repository.AdminKichThuocRepository;
import com.example.be.entity.KichThuoc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminKichThuocServiceImplTest {
    @Mock private AdminKichThuocRepository repository;
    @InjectMocks private AdminKichThuocServiceImpl service;
    @Test void testFindById() {
        KichThuoc entity = new KichThuoc();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertTrue(service.findById(1L).isPresent());
    }
}
