package com.example.be.core.admin.thuoctinh.degiay.service.impl;
import com.example.be.core.admin.thuoctinh.degiay.repository.AdminDeGiayRepository;
import com.example.be.entity.DeGiay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeGiayServiceImplTest {
    @Mock private AdminDeGiayRepository repository;
    @InjectMocks private AdminDeGiayServiceImpl service;
    @Test void testFindById() {
        DeGiay entity = new DeGiay();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        assertTrue(service.findById(1L).isPresent());
    }
}
