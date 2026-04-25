package com.example.be.unit.service;
import com.example.be.core.admin.phieugiamgia.repository.AdminPhieuGiamGiaRepository;
import com.example.be.core.admin.phieugiamgia.service.impl.AdminPhieuGiamGiaServiceImpl;
import com.example.be.entity.PhieuGiamGia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import com.example.be.core.admin.phieugiamgia.model.request.AdminPhieuGiamGiaRequest;
import static org.mockito.ArgumentMatchers.any;
import com.example.be.core.admin.phieugiamgia.model.response.AdminPhieuGiamGiaResponse;
import com.example.be.repository.KhachHangRepository;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import com.example.be.core.notification.EmailService;
import com.example.be.infrastructure.exceptions.ResourceNotFoundException;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminPhieuGiamGiaServiceImplTest {
    @Mock private AdminPhieuGiamGiaRepository repository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;
    @Mock private EmailService emailService;

    @InjectMocks private AdminPhieuGiamGiaServiceImpl service;

    @Test void testDetail() {
        AdminPhieuGiamGiaResponse mockResponse = mock(AdminPhieuGiamGiaResponse.class);
        when(repository.detail("1")).thenReturn(mockResponse);
        assertNotNull(service.detail("1"));
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
        assertThrows(ResourceNotFoundException.class, () -> service.update(new AdminPhieuGiamGiaRequest(), "999"));
    }
}
