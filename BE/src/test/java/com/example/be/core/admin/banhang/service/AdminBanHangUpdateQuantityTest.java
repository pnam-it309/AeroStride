package com.example.be.core.admin.banhang.service;

import com.example.be.core.admin.banhang.model.response.AdminBanHangHoaDonResponse;
import com.example.be.core.admin.banhang.repository.AdminBanHangChiTietSanPhamRepository;
import com.example.be.core.admin.banhang.repository.AdminBanHangHoaDonChiTietRepository;
import com.example.be.core.admin.banhang.repository.AdminBanHangHoaDonRepository;
import com.example.be.core.admin.banhang.repository.AdminBanHangPhieuGiamGiaRepository;
import com.example.be.core.admin.banhang.service.impl.AdminBanHangServiceImpl;
import com.example.be.core.admin.dotgiamgia.repository.AdminChiTietDotGiamGiaRepository;
import com.example.be.core.admin.sanpham.repository.AdminAnhChiTietSanPhamRepository;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.entity.PhieuGiamGia;
import com.example.be.entity.SanPham;
import com.example.be.infrastructure.exceptions.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBanHangUpdateQuantityTest {

    @Mock
    private AdminBanHangHoaDonRepository hoaDonRepository;
    @Mock
    private AdminBanHangHoaDonChiTietRepository hoaDonChiTietRepository;
    @Mock
    private AdminBanHangChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock
    private AdminBanHangPhieuGiamGiaRepository phieuGiamGiaRepository;
    @Mock
    private AdminChiTietDotGiamGiaRepository chiTietDotGiamGiaRepository;
    @Mock
    private AdminAnhChiTietSanPhamRepository anhChiTietSanPhamRepository;

    @InjectMocks
    private AdminBanHangServiceImpl service;

    private HoaDon hoaDon;
    private ChiTietSanPham variant;
    private HoaDonChiTiet oldPriceDetail;

    @BeforeEach
    void setUp() {
        hoaDon = HoaDon.builder()
                .id("order-1")
                .tongTien(new BigDecimal("100000"))
                .tongTienSauGiam(new BigDecimal("100000"))
                .build();

        SanPham sp = new SanPham();
        sp.setTen("Giày chạy bộ AeroStride Pro");

        variant = new ChiTietSanPham();
        variant.setId("variant-1");
        variant.setGiaBan(new BigDecimal("150000")); // Giá hiện tại đã đổi thành 150.000 đ
        variant.setSoLuong(10);
        variant.setSanPham(sp);

        oldPriceDetail = HoaDonChiTiet.builder()
                .hoaDon(hoaDon)
                .chiTietSanPham(variant)
                .soLuong(2)
                .donGia(new BigDecimal("100000")) // Giá cũ là 100.000 đ
                .giaGoc(new BigDecimal("100000"))
                .build();
        oldPriceDetail.setId("detail-1");
    }

    @Test
    void cannotIncreaseQuantityForOldPricedItem() {
        when(hoaDonChiTietRepository.findById("detail-1")).thenReturn(Optional.of(oldPriceDetail));
        when(chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(List.of("variant-1")))
                .thenReturn(Collections.emptyList());

        // Cố gắng tăng số lượng từ 2 lên 3 cho sản phẩm có giá cũ (100k vs 150k)
        BusinessException exception = assertThrows(BusinessException.class, () ->
                service.updateSoLuong("order-1", "detail-1", 3)
        );

        assertTrue(exception.getMessage().contains("Sản phẩm này đã đổi giá"));
        verify(hoaDonChiTietRepository, never()).save(any());
    }

    @Test
    void canDecreaseQuantityForOldPricedItem() {
        when(hoaDonChiTietRepository.findById("detail-1")).thenReturn(Optional.of(oldPriceDetail));
        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(hoaDon));
        when(chiTietSanPhamRepository.findByIdWithPessimisticLock("variant-1")).thenReturn(Optional.of(variant));
        when(chiTietSanPhamRepository.saveAndFlush(any())).thenReturn(variant);
        when(hoaDonChiTietRepository.findAllByHoaDon(hoaDon)).thenReturn(List.of(oldPriceDetail));
        when(chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(anyList())).thenReturn(Collections.emptyList());

        // Giảm số lượng từ 2 xuống 1 cho sản phẩm giá cũ
        AdminBanHangHoaDonResponse response = service.updateSoLuong("order-1", "detail-1", 1);

        assertNotNull(response);
        assertEquals(1, oldPriceDetail.getSoLuong());
        verify(hoaDonChiTietRepository).save(oldPriceDetail);
        // Kiểm tra mapping response
        assertTrue(response.getListsHoaDonChiTiet().get(0).getIsGiaCu());
        assertEquals(new BigDecimal("150000"), response.getListsHoaDonChiTiet().get(0).getGiaHienHanh());
    }

    @Test
    void canIncreaseQuantityForCurrentPricedItem() {
        HoaDonChiTiet currentPriceDetail = HoaDonChiTiet.builder()
                .hoaDon(hoaDon)
                .chiTietSanPham(variant)
                .soLuong(1)
                .donGia(new BigDecimal("150000")) // Giá hiện tại khớp với giá của biến thể
                .giaGoc(new BigDecimal("150000"))
                .build();
        currentPriceDetail.setId("detail-2");

        when(hoaDonChiTietRepository.findById("detail-2")).thenReturn(Optional.of(currentPriceDetail));
        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(hoaDon));
        when(chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(List.of("variant-1"))).thenReturn(Collections.emptyList());
        when(chiTietSanPhamRepository.findByIdWithPessimisticLock("variant-1")).thenReturn(Optional.of(variant));
        when(chiTietSanPhamRepository.saveAndFlush(any())).thenReturn(variant);
        when(hoaDonChiTietRepository.findAllByHoaDon(hoaDon)).thenReturn(List.of(currentPriceDetail));

        // Tăng số lượng từ 1 lên 2 cho sản phẩm giá mới
        AdminBanHangHoaDonResponse response = service.updateSoLuong("order-1", "detail-2", 2);

        assertNotNull(response);
        assertEquals(2, currentPriceDetail.getSoLuong());
        verify(hoaDonChiTietRepository).save(currentPriceDetail);
        assertFalse(response.getListsHoaDonChiTiet().get(0).getIsGiaCu());
    }

    @Test
    void detectsIneligibleVoucherWhenTotalDropsBelowMinimumOrder() {
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setId("voucher-500k");
        voucher.setMa("DISCOUNT500K");
        voucher.setTen("Giảm 50k cho đơn từ 500k");
        voucher.setDonHangToiThieu(new BigDecimal("500000"));
        voucher.setSoTienGiam(new BigDecimal("50000"));
        voucher.setLoaiPhieu("TIEN_MAT");
        voucher.setTrangThai(com.example.be.infrastructure.constants.TrangThai.DANG_HOAT_DONG);
        voucher.setSoLuong(10);

        hoaDon.setPhieuGiamGia(voucher);
        // Đơn hàng hiện tại chỉ có 1 sản phẩm 150k < 500k
        when(hoaDonChiTietRepository.findById("detail-1")).thenReturn(Optional.of(oldPriceDetail));
        when(hoaDonRepository.findById("order-1")).thenReturn(Optional.of(hoaDon));
        when(chiTietSanPhamRepository.findByIdWithPessimisticLock("variant-1")).thenReturn(Optional.of(variant));
        when(chiTietSanPhamRepository.saveAndFlush(any())).thenReturn(variant);
        when(hoaDonChiTietRepository.findAllByHoaDon(hoaDon)).thenReturn(List.of(oldPriceDetail));
        when(chiTietDotGiamGiaRepository.findAllByChiTietSanPhamIdIn(anyList())).thenReturn(Collections.emptyList());
        when(phieuGiamGiaRepository.findById("voucher-500k")).thenReturn(Optional.of(voucher));

        AdminBanHangHoaDonResponse response = service.updateSoLuong("order-1", "detail-1", 1);

        assertNotNull(response);
        assertTrue(response.getVoucherIneligible());
        assertEquals("MIN_ORDER_NOT_MET", response.getVoucherIneligibleReason());
        assertEquals(new BigDecimal("500000"), response.getVoucherMinOrder());
        assertNotNull(response.getVoucherIneligibleMessage());
    }
}
