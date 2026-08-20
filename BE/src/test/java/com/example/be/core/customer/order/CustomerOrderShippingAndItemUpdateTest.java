package com.example.be.core.customer.order;

import com.example.be.core.customer.order.model.request.CustomerUpdateItemsRequest;
import com.example.be.core.customer.order.model.request.CustomerUpdateShippingRequest;
import com.example.be.core.customer.order.model.response.CustomerOrderResponse;
import com.example.be.core.customer.order.repository.CustomerOrderChiTietSanPhamRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonChiTietRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderKhachHangRepository;
import com.example.be.core.customer.order.repository.CustomerOrderLichSuTrangThaiHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderPhieuGiamGiaRepository;
import com.example.be.core.customer.order.service.impl.CustomerOrderServiceImpl;
import com.example.be.entity.*;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.TrangThai;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderShippingAndItemUpdateTest {

    @Mock
    private CustomerOrderHoaDonRepository hoaDonRepository;
    @Mock
    private CustomerOrderHoaDonChiTietRepository hoaDonChiTietRepository;
    @Mock
    private CustomerOrderChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock
    private CustomerOrderKhachHangRepository khachHangRepository;
    @Mock
    private CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;
    @Mock
    private CustomerOrderPhieuGiamGiaRepository phieuGiamGiaRepository;

    @InjectMocks
    private CustomerOrderServiceImpl customerOrderService;

    private KhachHang khachHang;
    private HoaDon hoaDon;
    private ChiTietSanPham ctsp;
    private HoaDonChiTiet hdct;

    @BeforeEach
    void setUp() {
        khachHang = KhachHang.builder()
                .tenTaiKhoan("user1")
                .sdt("0912345678")
                .build();
        khachHang.setId("kh-1");
        khachHang.setTen("Nguyen Van A");

        hoaDon = HoaDon.builder()
                .maHoaDon("HD001")
                .khachHang(khachHang)
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

        SanPham sp = SanPham.builder().build();
        sp.setId("sp-1");
        sp.setTen("Nike Air Zoom");

        ctsp = ChiTietSanPham.builder()
                .giaBan(new BigDecimal("200000"))
                .soLuong(10)
                .sanPham(sp)
                .build();
        ctsp.setId("ctsp-1");

        hdct = HoaDonChiTiet.builder()
                .hoaDon(hoaDon)
                .chiTietSanPham(ctsp)
                .donGia(new BigDecimal("200000"))
                .soLuong(1)
                .build();
        hdct.setId("hdct-1");
    }

    @Test
    void testUpdateShippingInfo_FirstTime_Success() {
        when(khachHangRepository.findByTenTaiKhoan("user1")).thenReturn(Optional.of(khachHang));
        when(hoaDonRepository.findById("hd-1")).thenReturn(Optional.of(hoaDon));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerUpdateShippingRequest req = CustomerUpdateShippingRequest.builder()
                .tenNguoiNhan("Nguyen Van B")
                .soDienThoaiNguoiNhan("0987654321")
                .diaChiNguoiNhan("TP HCM")
                .ghiChu("Giao gio hanh chinh")
                .build();

        CustomerOrderResponse response = customerOrderService.updateShippingInfo("hd-1", req, "user1");

        assertNotNull(response);
        assertEquals("Nguyen Van B", hoaDon.getTenNguoiNhan());
        assertEquals("0987654321", hoaDon.getSoDienThoaiNguoiNhan());
        assertEquals("TP HCM", hoaDon.getDiaChiNguoiNhan());
        verify(lichSuRepository, times(1)).save(any(LichSuTrangThaiHoaDon.class));
    }

    @Test
    void testUpdateShippingInfo_SecondTime_ThrowsException() {
        // Giả lập lịch sử đã có 1 lần cập nhật thông tin nhận hàng
        LichSuTrangThaiHoaDon oldHistory = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .ghiChu("Khách hàng cập nhật thông tin nhận hàng: Tên: 'A' -> 'B'")
                .build();
        hoaDon.getListsLichSuHoaDon().add(oldHistory);

        when(khachHangRepository.findByTenTaiKhoan("user1")).thenReturn(Optional.of(khachHang));
        when(hoaDonRepository.findById("hd-1")).thenReturn(Optional.of(hoaDon));

        CustomerUpdateShippingRequest req = CustomerUpdateShippingRequest.builder()
                .tenNguoiNhan("Nguyen Van C")
                .soDienThoaiNguoiNhan("0987654321")
                .diaChiNguoiNhan("Da Nang")
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                customerOrderService.updateShippingInfo("hd-1", req, "user1")
        );

        assertTrue(ex.getMessage().contains("1 lần duy nhất"));
        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void testUpdateItems_PrepaidOrder_IncreasedQuantity_CausesSurcharge() {
        // Đơn đã thanh toán trước 230.000đ (200k tiền hàng + 30k ship)
        GiaoDichThanhToan gd = GiaoDichThanhToan.builder()
                .soTien(new BigDecimal("230000"))
                .maGiaoDichNgoai("VNPAY123")
                .build();
        gd.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
        hoaDon.getListsGiaoDichThanhToan().add(gd);

        when(khachHangRepository.findByTenTaiKhoan("user1")).thenReturn(Optional.of(khachHang));
        when(hoaDonRepository.findById("hd-1")).thenReturn(Optional.of(hoaDon));
        when(hoaDonChiTietRepository.findAllByHoaDon(hoaDon)).thenReturn(Collections.singletonList(hdct));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));

        // Tăng số lượng từ 1 lên 2
        CustomerUpdateItemsRequest req = CustomerUpdateItemsRequest.builder()
                .items(Collections.singletonList(
                        CustomerUpdateItemsRequest.Item.builder()
                                .idChiTietSanPham("ctsp-1")
                                .soLuong(2)
                                .build()
                ))
                .build();

        CustomerOrderResponse response = customerOrderService.updateItems("hd-1", req, "user1");

        assertNotNull(response);
        assertEquals(2, hdct.getSoLuong());
        // Tổng tiền mới: 2 * 200.000 + 30.000 = 430.000
        assertEquals(new BigDecimal("430000"), hoaDon.getTongTienSauGiam());
        verify(lichSuRepository, times(1)).save(argThat(ls ->
                ls.getGhiChu().contains("Phát sinh phụ phí")
        ));
    }

    @Test
    void testUpdateItems_PrepaidOrder_DecreasedQuantity_CausesRefund() {
        // Đơn gồm 2 sản phẩm tổng 430.000đ đã thanh toán
        hdct.setSoLuong(2);
        hoaDon.setTongTien(new BigDecimal("400000"));
        hoaDon.setTongTienSauGiam(new BigDecimal("430000"));

        GiaoDichThanhToan gd = GiaoDichThanhToan.builder()
                .soTien(new BigDecimal("430000"))
                .maGiaoDichNgoai("VNPAY123")
                .build();
        gd.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
        hoaDon.getListsGiaoDichThanhToan().add(gd);

        when(khachHangRepository.findByTenTaiKhoan("user1")).thenReturn(Optional.of(khachHang));
        when(hoaDonRepository.findById("hd-1")).thenReturn(Optional.of(hoaDon));
        when(hoaDonChiTietRepository.findAllByHoaDon(hoaDon)).thenReturn(Collections.singletonList(hdct));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));

        // Giảm số lượng từ 2 xuống 1
        CustomerUpdateItemsRequest req = CustomerUpdateItemsRequest.builder()
                .items(Collections.singletonList(
                        CustomerUpdateItemsRequest.Item.builder()
                                .idChiTietSanPham("ctsp-1")
                                .soLuong(1)
                                .build()
                ))
                .build();

        CustomerOrderResponse response = customerOrderService.updateItems("hd-1", req, "user1");

        assertNotNull(response);
        assertEquals(1, hdct.getSoLuong());
        // Tổng tiền mới: 1 * 200.000 + 30.000 = 230.000 -> hoàn 200.000đ
        assertEquals(new BigDecimal("230000"), hoaDon.getTongTienSauGiam());
        assertEquals(Boolean.FALSE, hoaDon.getDaHoanPhi());
        assertEquals(new BigDecimal("200000"), hoaDon.getPhiHoanHang());
        verify(lichSuRepository, times(1)).save(argThat(ls ->
                ls.getGhiChu().contains("Phát sinh tiền hoàn")
        ));
    }
}
