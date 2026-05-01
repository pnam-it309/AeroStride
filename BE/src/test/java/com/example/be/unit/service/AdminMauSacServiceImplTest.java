package com.example.be.unit.service;

import com.example.be.core.admin.thuoctinh.mausac.repository.AdminMauSacRepository;
import com.example.be.core.admin.thuoctinh.mausac.service.impl.AdminMauSacServiceImpl;
import com.example.be.entity.MauSac;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.data.jpa.domain.Specification;
import com.example.be.core.admin.thuoctinh.model.request.AdminAttributeRequest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMauSacServiceImplTest {

    @Mock
    private AdminMauSacRepository repository;

    @InjectMocks
    private AdminMauSacServiceImpl service;

    private MauSac testTarget;

    @BeforeEach
    void setUp() {
        testTarget = new MauSac();
        testTarget.setId("1");
        testTarget.setMaMauHex("#FFFFFF");
        testTarget.setXoaMem(false);
    }

    @Test
    void testGetById_Success() {
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(testTarget));

        var result = service.getById("1");

        assertNotNull(result);
        assertEquals("#FFFFFF", result.getMoTa());
    }

    @Test
    void testDelete_ShouldSoftDelete() {
        when(repository.findOne(any(Specification.class))).thenReturn(Optional.of(testTarget));

        service.delete("1");

        verify(repository).save(testTarget);
    }

    @Test
    void testSave_ShouldPersistEntity() {
        AdminAttributeRequest request = new AdminAttributeRequest();
        request.setTen("Red");
        request.setMoTa("#FF0000");

        when(repository.save(any(MauSac.class))).thenReturn(testTarget);

        var result = service.create(request);

        assertNotNull(result);
        verify(repository).save(any(MauSac.class));
    }
}
