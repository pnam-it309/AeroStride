package com.example.be.unit.service;
import com.example.be.core.admin.thuoctinh.kichthuoc.repository.AdminKichThuocRepository;
import com.example.be.core.admin.thuoctinh.kichthuoc.service.impl.AdminKichThuocServiceImpl;
import com.example.be.entity.KichThuoc;
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
class AdminKichThuocServiceImplTest {
    @Mock private AdminKichThuocRepository repository;
    @InjectMocks private AdminKichThuocServiceImpl service;
    @Test void testGetById() {
        KichThuoc entity = new KichThuoc();
        entity.setId("1");
        entity.setMa("KT1");
        entity.setTen("Size 1");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        assertNotNull(service.getById("1"));
    }
}
