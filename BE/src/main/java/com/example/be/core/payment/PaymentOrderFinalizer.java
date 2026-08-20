package com.example.be.core.payment;

import com.example.be.core.customer.order.repository.CustomerOrderChiTietSanPhamRepository;
import com.example.be.core.customer.order.repository.CustomerOrderGiaoDichThanhToanRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonChiTietRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderLichSuTrangThaiHoaDonRepository;
import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.OrderType;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ghi nhận kết quả thanh toán thành công từ cổng (VNPay) vào DB:
 * Cập nhật trạng thái giao dịch thanh toán, ghi nhận lịch sử đơn hàng.
 * Đơn hàng trực tuyến giữ trạng thái CHO_XAC_NHAN (tồn kho sẽ được trừ khi Admin xác nhận đơn).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderFinalizer {

    private final CustomerOrderHoaDonRepository hoaDonRepository;
    private final CustomerOrderHoaDonChiTietRepository hoaDonChiTietRepository;
    private final CustomerOrderChiTietSanPhamRepository chiTietSanPhamRepository;
    private final CustomerOrderGiaoDichThanhToanRepository giaoDichRepository;
    private final CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public void markPaid(String orderId, Map<String, String> params) {
        if (orderId == null || orderId.isBlank()) return;

        HoaDon hoaDon = hoaDonRepository.findById(orderId).orElse(null);
        if (hoaDon == null) {
            log.warn("VNPay callback: không tìm thấy hóa đơn id={}", orderId);
            return;
        }

        // Idempotency: nếu đơn đã bị hủy hoặc hoàn tất thì bỏ qua
        if (hoaDon.getTrangThai() == OrderStatus.DA_HUY || hoaDon.getTrangThai() == OrderStatus.HOAN_THANH) {
            log.info("VNPay callback: hóa đơn {} ở trạng thái {}, bỏ qua.", orderId, hoaDon.getTrangThai());
            return;
        }

        String vnpTxnNo = params.get("vnp_TransactionNo");
        String vnpTxnRef = params.get("vnp_TxnRef");

        // Cập nhật các giao dịch thanh toán của đơn -> đã thanh toán thành công
        if (hoaDon.getListsGiaoDichThanhToan() != null) {
            for (GiaoDichThanhToan gd : hoaDon.getListsGiaoDichThanhToan()) {
                gd.setTrangThai(TrangThai.NGUNG_HOAT_DONG); // quy ước: đã hoàn tất/đã thanh toán
                gd.setMaGiaoDichNgoai(vnpTxnNo);
                gd.setMaThamChieu(vnpTxnRef);
                gd.setGhiChu("Đã thanh toán qua VNPay" + (vnpTxnNo != null ? " - GD: " + vnpTxnNo : ""));
                giaoDichRepository.save(gd);
            }
        }

        hoaDon.setNgayCapNhat(System.currentTimeMillis());
        hoaDonRepository.save(hoaDon);

        String txnIdentifier = vnpTxnNo != null ? vnpTxnNo : (vnpTxnRef != null ? vnpTxnRef : "Thành công");
        String ghiChuLichSu = "Khách hàng đã thanh toán thành công qua VNPay (Mã GD: " + txnIdentifier + "). Đang chờ Admin xác nhận đơn hàng.";

        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().ordinal() : OrderStatus.CHO_XAC_NHAN.ordinal())
                .trangThaiMoi(hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().ordinal() : OrderStatus.CHO_XAC_NHAN.ordinal())
                .ghiChu(ghiChuLichSu)
                .nguoiThucHien("Khách hàng")
                .build();
        lichSuRepository.save(lichSu);

        log.info("VNPay callback: đã ghi nhận thanh toán cho hóa đơn id={}, vnpTxnNo={}", orderId, vnpTxnNo);

        String email = hoaDon.getEmailNguoiNhan() != null && !hoaDon.getEmailNguoiNhan().isBlank()
                ? hoaDon.getEmailNguoiNhan().trim()
                : (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getEmail() != null ? hoaDon.getKhachHang().getEmail().trim() : null);
        if (isOnlineOrder(hoaDon) && email != null && !email.isBlank()) {
            eventPublisher.publishEvent(new com.example.be.core.common.events.OrderPlacedEvent(
                    this, hoaDon.getId(), email, hoaDon.getTongTienSauGiam()));
            log.info("Published OrderPlacedEvent after VNPay payment for order {} to email {}", hoaDon.getId(), email);
        }
    }

    private boolean isOnlineOrder(HoaDon hoaDon) {
        if (hoaDon.getOrderType() != null) {
            return hoaDon.getOrderType() == OrderType.ONLINE;
        }
        return hoaDon.getNhanVien() == null && OrderType.ONLINE.name().equalsIgnoreCase(hoaDon.getLoaiDon());
    }
}
