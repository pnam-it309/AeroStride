package com.example.be.core.admin.thuoctinh.danhmuc.service.impl;
import com.example.be.core.admin.thuoctinh.danhmuc.repository.AdminDanhMucRepository;
import com.example.be.entity.DanhMuc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDanhMucServiceImplTest {
    @Mock private AdminDanhMucRepository repository;
    @InjectMocks private AdminDanhMucServiceImpl service;
    @Test void testGetById() {
        DanhMuc entity = new DanhMuc();
        entity.setId("1");
        entity.setMa("DM1");
        entity.setTen("Category 1");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        assertNotNull(service.getById("1"));
    }
}
