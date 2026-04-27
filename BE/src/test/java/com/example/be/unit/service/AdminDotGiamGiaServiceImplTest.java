package com.example.be.unit.service;

import com.example.be.core.admin.dotgiamgia.model.request.AdminDotGiamGiaRequest;
import com.example.be.core.admin.dotgiamgia.repository.AdminDotGiamGiaRepository;
import com.example.be.core.admin.dotgiamgia.service.impl.AdminDotGiamGiaServiceImpl;
import com.example.be.entity.DotGiamGia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import com.example.be.core.admin.sanpham.mapper.AdminSanPhamMapper;
import com.example.be.core.admin.sanpham.repository.AdminChiTietSanPhamRepository;
import com.example.be.repository.ChiTietDotGiamGiaRepository;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDotGiamGiaServiceImplTest {

    @Mock
    private AdminDotGiamGiaRepository repo;

    @Mock
    private AdminChiTietSanPhamRepository chiTietSanPhamRepo;

    @Mock
    private AdminSanPhamMapper mapper;

    @Mock
    private ChiTietDotGiamGiaRepository chiTietDotGiamGiaRepo;

    @InjectMocks
    private AdminDotGiamGiaServiceImpl service;

    private AdminDotGiamGiaRequest testRequest;
    private DotGiamGia testEntity;

    @BeforeEach
    void setUp() {
        testRequest = new AdminDotGiamGiaRequest();
        testRequest.setMa("DGG001");
        testRequest.setTen("Săn sale Black Friday");

        testEntity = new DotGiamGia();
        testEntity.setId("1");
        testEntity.setMa("DGG001");
    }

    @Test
    void add_ShouldSaveEntity() {
        service.add(testRequest);

        verify(repo).save(any(DotGiamGia.class));
    }

    @Test
    void update_ShouldThrowException_WhenNotFound() {
        when(repo.findById("999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(testRequest, "999"));
    }

    @Test
    void update_ShouldSaveModifiedEntity() {
        when(repo.findById("1")).thenReturn(Optional.of(testEntity));

        service.update(testRequest, "1");

        assertEquals("DGG001", testEntity.getMa());
        verify(repo).save(testEntity);
    }

    @Test
    void delete_ShouldCallRepository() {
        when(repo.existsById("1")).thenReturn(true);
        service.delete("1");

        verify(repo).deleteById("1");
    }
}
