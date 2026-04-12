package com.example.be.core.admin.phieugiamgia.service.impl;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.entity.PhieuGiamGia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPhieuGiamGiaServiceImplTest {
    @Mock private AdminPhieuGiamGiaRepository repository;
    @InjectMocks private AdminPhieuGiamGiaServiceImpl service;
    @Test void testFindById() {
        PhieuGiamGia entity = new PhieuGiamGia();
        when(repository.findById("1")).thenReturn(Optional.of(entity));
        assertTrue(service.findById("1").isPresent());
    }

    @Test
    void testUpdate_Success() {
        PhieuGiamGia entity = new PhieuGiamGia();
        when(repository.findById("1")).thenReturn(Optional.of(entity));
        service.update(new AdminPhieuGiamGiaRequest(), "1");
        verify(repository).save(any(PhieuGiamGia.class));
    }

    @Test
    void testUpdate_NotFound_ShouldThrowException() {
        when(repository.findById("999")).thenReturn(Optional.empty());
        // Bài test này sẽ chứng minh code hiện tại của bạn bị lỗi nếu ID không tồn tại
        assertThrows(java.util.NoSuchElementException.class, () -> service.update(new AdminPhieuGiamGiaRequest(), "999"));
    }
}
