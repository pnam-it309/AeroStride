package com.example.be.core.admin.thuoctinh.degiay.service.impl;
import com.example.be.core.admin.thuoctinh.degiay.repository.AdminDeGiayRepository;
import com.example.be.entity.DeGiay;
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
class AdminDeGiayServiceImplTest {
    @Mock private AdminDeGiayRepository repository;
    @InjectMocks private AdminDeGiayServiceImpl service;
    @Test void testGetById() {
        DeGiay entity = new DeGiay();
        entity.setId("1");
        entity.setMa("DG1");
        entity.setTen("Sole 1");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        assertNotNull(service.getById("1"));
    }
}
