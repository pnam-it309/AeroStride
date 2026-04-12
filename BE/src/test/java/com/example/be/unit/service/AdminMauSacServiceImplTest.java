package com.example.be.core.admin.thuoctinh.mausac.service.impl;

import com.example.be.core.admin.thuoctinh.mausac.repository.AdminMauSacRepository;
import com.example.be.entity.MauSac;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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
        testTarget.setId(1L);
        testTarget.setMaMauHex("#FFFFFF");
        testTarget.setXoaMem(false);
    }

    @Test
    void testFindById_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTarget));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("#FFFFFF", result.get().getMaMauHex());
    }

    @Test
    void testDelete_ShouldSoftDelete() {
        when(repository.findById(1L)).thenReturn(Optional.of(testTarget));
        
        service.delete(1L);

        assertTrue(testTarget.getXoaMem());
        verify(repository).save(testTarget);
    }

    @Test
    void testSave_ShouldPersistEntity() {
        when(repository.save(any(MauSac.class))).thenReturn(testTarget);

        var result = service.create(testTarget);

        assertNotNull(result);
        verify(repository).save(any(MauSac.class));
    }
}
