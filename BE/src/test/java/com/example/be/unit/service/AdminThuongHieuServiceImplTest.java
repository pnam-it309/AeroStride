package com.example.be.core.admin.thuoctinh.thuonghieu.service.impl;
import com.example.be.core.admin.thuoctinh.thuonghieu.repository.AdminThuongHieuRepository;
import com.example.be.entity.ThuongHieu;
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
class AdminThuongHieuServiceImplTest {
    @Mock private AdminThuongHieuRepository repository;
    @InjectMocks private AdminThuongHieuServiceImpl service;
    @Test void testGetById() {
        ThuongHieu entity = new ThuongHieu();
        entity.setId("1");
        entity.setMa("TH1");
        entity.setTen("Brand 1");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        assertNotNull(service.getById("1"));
    }
}
