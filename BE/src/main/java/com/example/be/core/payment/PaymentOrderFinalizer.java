package com.example.be.core.payment;

import com.example.be.core.customer.order.repository.CustomerOrderGiaoDichThanhToanRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderLichSuTrangThaiHoaDonRepository;
import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Ghi nhận kết quả thanh toán thành công từ cổng (VNPay) vào DB:
 * cập nhật trạng thái giao dịch của hóa đơn + ghi lịch sử.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrderFinalizer {

    private final CustomerOrderHoaDonRepository hoaDonRepository;
    private final CustomerOrderGiaoDichThanhToanRepository giaoDichRepository;
    private final CustomerOrderLichSuTrangThaiHoaDonRepository lichSuRepository;

    @Transactional
    public void markPaid(String orderId, Map<String, String> params) {
        if (orderId == null || orderId.isBlank()) return;

        HoaDon hoaDon = hoaDonRepository.findById(orderId).orElse(null);
        if (hoaDon == null) {
            log.warn("VNPay callback: không tìm thấy hóa đơn id={}", orderId);
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

        // Lịch sử: ghi rõ khách hàng đã thanh toán
        Integer trangThaiOrdinal = hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().ordinal() : null;
        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(trangThaiOrdinal)
                .trangThaiMoi(trangThaiOrdinal)
                .ghiChu("Khách hàng đã thanh toán qua VNPay")
                .nguoiThucHien("Khách hàng")
                .build();
        lichSuRepository.save(lichSu);

        log.info("VNPay callback: đã ghi nhận thanh toán cho hóa đơn id={}, vnpTxnNo={}", orderId, vnpTxnNo);
    }
}
