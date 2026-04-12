package com.example.be.core.admin.thuoctinh.mucdichchay.service.impl;
import com.example.be.core.admin.thuoctinh.mucdichchay.repository.AdminMucDichChayRepository;
import com.example.be.entity.MucDichChay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMucDichChayServiceImplTest {
    @Mock private AdminMucDichChayRepository repository;
    @InjectMocks private AdminMucDichChayServiceImpl service;
    @Test void testFindById() {
        MucDichChay entity = new MucDichChay();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertTrue(service.findById(1L).isPresent());
    }
}
