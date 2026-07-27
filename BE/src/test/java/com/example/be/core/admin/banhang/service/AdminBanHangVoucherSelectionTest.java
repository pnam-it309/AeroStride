package com.example.be.core.admin.banhang.service;

import com.example.be.core.admin.banhang.repository.AdminBanHangHoaDonRepository;
import com.example.be.core.admin.banhang.repository.AdminBanHangPhieuGiamGiaRepository;
import com.example.be.core.admin.banhang.service.impl.AdminBanHangServiceImpl;
import com.example.be.entity.HoaDon;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.infrastructure.exceptions.BusinessException;
import com.example.be.repository.PhieuGiamGiaCaNhanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBanHangVoucherSelectionTest {

    @Mock
    private AdminBanHangHoaDonRepository hoaDonRepository;
    @Mock
    private AdminBanHangPhieuGiamGiaRepository phieuGiamGiaRepository;
    @Mock
    private PhieuGiamGiaCaNhanRepository phieuGiamGiaCaNhanRepository;

    @InjectMocks
    private AdminBanHangServiceImpl service;

    @Test
    void bestVoucherUsesActualDiscountSoPercentageVoucherCanWin() {
        HoaDon order = HoaDon.builder()
                .id("order-1")
                .tongTien(new BigDecimal("1000000"))
                .build();
        PhieuGiamGia fixed = activeVoucher("fixed", "TIEN_MAT");
        fixed.setSoTienGiam(new BigDecimal("100000"));

        PhieuGiamGia percentage = activeVoucher("percent", "PHAN_TRAM");
        percentage.setPhanTramGiamGia(20);
        percentage.setGiamToiDa(new BigDecimal("300000"));

        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG))
                .thenReturn(List.of(fixed, percentage));

        assertEquals("percent", service.getBestVoucher("order-1").getId());
    }

    @Test
    void cannotApplyPersonalVoucherThatDoesNotBelongToCustomer() {
        HoaDon order = HoaDon.builder()
                .id("order-1")
                .tongTien(new BigDecimal("1000000"))
                .build();
        PhieuGiamGia personal = activeVoucher("personal", "TIEN_MAT");
        personal.setHinhThuc("CA_NHAN");
        personal.setSoTienGiam(new BigDecimal("200000"));

        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(phieuGiamGiaRepository.findById("personal")).thenReturn(Optional.of(personal));

        assertThrows(BusinessException.class, () -> service.setPhieuGiamGia("order-1", "personal"));
    }

    @Test
    void publicUnlimitedVoucherWithMinusOneQuantityIsEligible() {
        HoaDon order = HoaDon.builder()
                .id("order-1")
                .tongTien(new BigDecimal("990000"))
                .build();
        PhieuGiamGia unlimited = activeVoucher("unlimited", "TIEN_MAT");
        unlimited.setSoLuong(-1);
        unlimited.setSoTienGiam(new BigDecimal("100000"));
        unlimited.setDonHangToiThieu(new BigDecimal("100000"));

        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG))
                .thenReturn(List.of(unlimited));

        assertEquals("unlimited", service.getBestVoucher("order-1").getId());
    }

    @Test
    void zeroQuantityVoucherIsNotEligible() {
        HoaDon order = HoaDon.builder()
                .id("order-1")
                .tongTien(new BigDecimal("990000"))
                .build();
        PhieuGiamGia exhausted = activeVoucher("exhausted", "TIEN_MAT");
        exhausted.setSoLuong(0);
        exhausted.setSoTienGiam(new BigDecimal("100000"));

        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(order));
        when(phieuGiamGiaRepository.findAllByTrangThai(TrangThai.DANG_HOAT_DONG))
                .thenReturn(List.of(exhausted));

        assertEquals(null, service.getBestVoucher("order-1"));
    }

    private PhieuGiamGia activeVoucher(String id, String type) {
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId(id);
        voucher.setLoaiPhieu(type);
        voucher.setHinhThuc("CONG_KHAI");
        voucher.setTrangThai(TrangThai.DANG_HOAT_DONG);
        voucher.setSoLuong(10);
        voucher.setDonHangToiThieu(BigDecimal.ZERO);
        return voucher;
    }
}
