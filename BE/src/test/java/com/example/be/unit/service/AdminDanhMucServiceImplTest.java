package com.example.be.core.admin.thuoctinh.danhmuc.service.impl;
import com.example.be.core.admin.thuoctinh.danhmuc.repository.AdminDanhMucRepository;
import com.example.be.entity.DanhMuc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDanhMucServiceImplTest {
    @Mock private AdminDanhMucRepository repository;
    @InjectMocks private AdminDanhMucServiceImpl service;
    @Test void testFindById() {
        DanhMuc entity = new DanhMuc();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertTrue(service.findById(1L).isPresent());
    }
}
