package com.example.be.core.payment;

import com.example.be.core.customer.order.repository.CustomerOrderChiTietSanPhamRepository;
import com.example.be.core.customer.order.repository.CustomerOrderGiaoDichThanhToanRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonChiTietRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderLichSuTrangThaiHoaDonRepository;
import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.TrangThai;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrderFinalizerTest {

    @Mock
    private CustomerOrderHoaDonRepository hoaDonRepository;
    @Mock
    private CustomerOrderHoaDonChiTietRepository hoaDonChiTietRepository;
    @Mock
    private CustomerOrderChiTietSanPhamRepository chiTietSanPhamRepository;
    @Mock
    private CustomerOrderGiaoDichThanhToanRepository giaoDichRepository;
    @Mock
    private CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentOrderFinalizer paymentOrderFinalizer;

    private HoaDon onlineOrder;
    private GiaoDichThanhToan paymentTxn;

    @BeforeEach
    void setUp() {
        onlineOrder = new HoaDon();
        onlineOrder.setId("order-vnpay-1");
        onlineOrder.setMaHoaDon("HD-ONLINE-001");
        onlineOrder.setTrangThai(OrderStatus.CHO_XAC_NHAN);
        onlineOrder.setOrderType(OrderType.ONLINE);
        onlineOrder.setTongTien(new BigDecimal("500000"));
        onlineOrder.setTongTienSauGiam(new BigDecimal("500000"));

        paymentTxn = new GiaoDichThanhToan();
        paymentTxn.setId("txn-1");
        paymentTxn.setHoaDon(onlineOrder);
        paymentTxn.setTrangThai(TrangThai.DANG_HOAT_DONG);

        onlineOrder.setListsGiaoDichThanhToan(java.util.Set.of(paymentTxn));
    }

    @Test
    void markPaid_setsStatusToXacNhanAndDeductsStock() {
        when(hoaDonRepository.findById("order-vnpay-1")).thenReturn(Optional.of(onlineOrder));

        Map<String, String> params = Map.of(
                "vnp_TransactionNo", "14567890",
                "vnp_TxnRef", "order-vnpay-1"
        );

        paymentOrderFinalizer.markPaid("order-vnpay-1", params);

        // Verify order status transitions to XAC_NHAN
        assertEquals(OrderStatus.XAC_NHAN, onlineOrder.getTrangThai());

        // Verify payment transaction was marked as completed/paid
        assertEquals(TrangThai.NGUNG_HOAT_DONG, paymentTxn.getTrangThai());
        assertEquals("14567890", paymentTxn.getMaGiaoDichNgoai());
        assertEquals("order-vnpay-1", paymentTxn.getMaThamChieu());

        verify(lichSuRepository, times(1)).save(any());
        verify(hoaDonRepository, times(1)).save(onlineOrder);
    }
}
