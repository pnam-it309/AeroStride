package com.example.be.core.admin.hoadon;

import com.example.be.core.admin.hoadon.mapper.AdminHoaDonMapper;
import com.example.be.core.admin.hoadon.model.request.AdminUpdateHoaDonRequest;
import com.example.be.core.admin.hoadon.model.response.AdminHoaDonDetailResponse;
import com.example.be.core.admin.hoadon.repository.AdminHoaDonRepository;
import com.example.be.core.admin.hoadon.service.impl.AdminHoaDonServiceImpl;
import com.example.be.entity.HoaDon;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.repository.LichSuTrangThaiHoaDonRepository;
import com.example.be.repository.NhanVienRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminHoaDonInfoAndRefundTest {

    @Mock
    private AdminHoaDonRepository repository;
    @Mock
    private AdminHoaDonMapper hoaDonMapper;
    @Mock
    private LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;
    @Mock
    private NhanVienRepository nhanVienRepository;

    @InjectMocks
    private AdminHoaDonServiceImpl adminHoaDonService;

    private HoaDon hoaDon;

    @BeforeEach
    void setUp() {
        hoaDon = HoaDon.builder()
                .maHoaDon("HD001")
                .trangThai(OrderStatus.CHO_XAC_NHAN)
                .orderType(OrderType.ONLINE)
                .tenNguoiNhan("Nguyen Van A")
                .soDienThoaiNguoiNhan("0912345678")
                .diaChiNguoiNhan("Ha Noi")
                .tongTien(new BigDecimal("200000"))
                .phiVanChuyen(new BigDecimal("30000"))
                .tongTienSauGiam(new BigDecimal("230000"))
                .listsLichSuHoaDon(new HashSet<>())
                .listsGiaoDichThanhToan(new HashSet<>())
                .build();
        hoaDon.setId("hd-1");
    }

    @Test
    void testAdminUpdateInfo_FirstTime_Success() {
        when(repository.findDetailById("hd-1")).thenReturn(Optional.of(hoaDon));
        when(repository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hoaDonMapper.toDetailResponse(hoaDon)).thenReturn(AdminHoaDonDetailResponse.builder().build());

        AdminUpdateHoaDonRequest req = AdminUpdateHoaDonRequest.builder()
                .tenNguoiNhan("Nguyen Van B")
                .soDienThoaiNguoiNhan("0987654321")
                .diaChiNguoiNhan("TP HCM")
                .build();

        AdminHoaDonDetailResponse response = adminHoaDonService.updateInfo("hd-1", req);

        assertNotNull(response);
        assertEquals("Nguyen Van B", hoaDon.getTenNguoiNhan());
        assertEquals("0987654321", hoaDon.getSoDienThoaiNguoiNhan());
        assertEquals("TP HCM", hoaDon.getDiaChiNguoiNhan());
        verify(lichSuTrangThaiHoaDonRepository, times(1)).save(any(LichSuTrangThaiHoaDon.class));
    }

    @Test
    void testAdminUpdateInfo_SecondTime_ThrowsBusinessException() {
        LichSuTrangThaiHoaDon oldLog = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .ghiChu("Cập nhật thông tin giao hàng/khách hàng: Tên: 'A' -> 'B'")
                .build();
        hoaDon.getListsLichSuHoaDon().add(oldLog);

        when(repository.findDetailById("hd-1")).thenReturn(Optional.of(hoaDon));

        AdminUpdateHoaDonRequest req = AdminUpdateHoaDonRequest.builder()
                .tenNguoiNhan("Nguyen Van C")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                adminHoaDonService.updateInfo("hd-1", req)
        );

        assertTrue(ex.getMessage().contains("1 lần duy nhất"));
        verify(repository, never()).save(any(HoaDon.class));
    }

    @Test
    void testConfirmRefund_WithPendingRefundSurplus_Success() {
        hoaDon.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        hoaDon.setDaHoanPhi(false);
        hoaDon.setPhiHoanHang(new BigDecimal("50000"));

        when(repository.findDetailById("hd-1")).thenReturn(Optional.of(hoaDon));
        when(repository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hoaDonMapper.toDetailResponse(hoaDon)).thenReturn(AdminHoaDonDetailResponse.builder().build());

        AdminHoaDonDetailResponse response = adminHoaDonService.confirmRefund("hd-1");

        assertNotNull(response);
        assertEquals(Boolean.TRUE, hoaDon.getDaHoanPhi());
        verify(lichSuTrangThaiHoaDonRepository, times(1)).save(argThat(log ->
                log.getGhiChu().contains("Xác nhận đã hoàn phí")
        ));
    }
}
