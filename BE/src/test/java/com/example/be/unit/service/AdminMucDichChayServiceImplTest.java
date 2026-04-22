package com.example.be.core.admin.thuoctinh.mucdichchay.service.impl;
import com.example.be.core.admin.thuoctinh.mucdichchay.repository.AdminMucDichChayRepository;
import com.example.be.entity.MucDichChay;
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
class AdminMucDichChayServiceImplTest {
    @Mock private AdminMucDichChayRepository repository;
    @InjectMocks private AdminMucDichChayServiceImpl service;
    @Test void testGetById() {
        MucDichChay entity = new MucDichChay();
        entity.setId("1");
        entity.setMa("MDC1");
        entity.setTen("Purpose 1");
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(entity));
        assertNotNull(service.getById("1"));
    }
}
