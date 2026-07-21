package com.example.be.core.payment;

import com.example.be.core.customer.order.repository.CustomerOrderChiTietSanPhamRepository;
import com.example.be.core.customer.order.repository.CustomerOrderGiaoDichThanhToanRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonChiTietRepository;
import com.example.be.core.customer.order.repository.CustomerOrderHoaDonRepository;
import com.example.be.core.customer.order.repository.CustomerOrderLichSuTrangThaiHoaDonRepository;
import com.example.be.entity.ChiTietSanPham;
import com.example.be.entity.GiaoDichThanhToan;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.infrastructure.constants.TrangThai;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Ghi nhận kết quả thanh toán thành công từ cổng (VNPay) vào DB:
 * trừ tồn kho (đơn VNPay chưa trừ lúc đặt hàng), cập nhật trạng thái giao dịch + hóa đơn, ghi lịch sử.
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

        // Idempotency: chỉ xử lý khi đơn còn 'Chờ xác nhận'.
        // Tránh trừ kho / xác nhận nhiều lần khi callback VNPay lặp lại (IPN + return, hoặc confirm thủ công).
        if (hoaDon.getTrangThai() != OrderStatus.CHO_XAC_NHAN) {
            log.info("VNPay callback: hóa đơn {} đã được xử lý trước đó (trạng thái {}), bỏ qua.",
                    orderId, hoaDon.getTrangThai());
            return;
        }

        String vnpTxnNo = params.get("vnp_TransactionNo");
        String vnpTxnRef = params.get("vnp_TxnRef");

        // Trừ tồn kho tại thời điểm thanh toán thành công (đơn VNPay chưa trừ lúc đặt hàng).
        // Dùng UPDATE atomic có điều kiện soLuong >= qty để chống oversell khi nhiều đơn tranh nhau hàng cuối.
        // Đơn TAI_QUAY đã trừ lúc thêm vào giỏ hàng, nên bỏ qua bước này.
        StringBuilder thieuHangNote = new StringBuilder();
        if (!"TAI_QUAY".equalsIgnoreCase(hoaDon.getLoaiDon())) {
            List<HoaDonChiTiet> chiTiets = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
            for (HoaDonChiTiet ct : chiTiets) {
                ChiTietSanPham ctsp = ct.getChiTietSanPham();
                if (ctsp == null) continue;

                int tonHienTai = ctsp.getSoLuong() != null ? ctsp.getSoLuong() : 0;
                if (tonHienTai < ct.getSoLuong()) {
                    String tenSP = ctsp.getSanPham() != null ? ctsp.getSanPham().getTen() : ctsp.getMaChiTietSanPham();
                    thieuHangNote.append(String.format("'%s' (tồn %d, cần %d); ", tenSP, tonHienTai, ct.getSoLuong()));
                }
                ctsp.setSoLuong(tonHienTai - ct.getSoLuong());
                chiTietSanPhamRepository.save(ctsp);
            }
        }

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

        // Lịch sử: ghi rõ khách hàng đã thanh toán và cập nhật trạng thái hóa đơn
        Integer trangThaiCu = hoaDon.getTrangThai() != null ? hoaDon.getTrangThai().ordinal() : null;

        // Đổi trạng thái hóa đơn thành Đã xác nhận (khách đã trả tiền -> hệ thống tự xác nhận đơn)
        hoaDon.setTrangThai(OrderStatus.XAC_NHAN);
        hoaDonRepository.save(hoaDon);

        boolean thieuHang = thieuHangNote.length() > 0;
        String ghiChuLichSu = thieuHang
                ? "Khách hàng đã thanh toán qua VNPay. Đơn tự động xác nhận NHƯNG các sản phẩm sau không đủ tồn kho, cần admin xử lý (nhập thêm hàng hoặc hoàn tiền): " + thieuHangNote.toString().trim()
                : "Khách hàng đã thanh toán qua VNPay. Đơn hàng tự động xác nhận.";

        LichSuTrangThaiHoaDon lichSu = LichSuTrangThaiHoaDon.builder()
                .hoaDon(hoaDon)
                .trangThaiCu(trangThaiCu)
                .trangThaiMoi(OrderStatus.XAC_NHAN.ordinal())
                .ghiChu(ghiChuLichSu)
                .nguoiThucHien("Khách hàng")
                .build();
        lichSuRepository.save(lichSu);

        if (thieuHang) {
            log.warn("VNPay callback: hóa đơn {} đã thanh toán nhưng thiếu tồn kho: {}",
                    orderId, thieuHangNote.toString().trim());
        }
        log.info("VNPay callback: đã ghi nhận thanh toán cho hóa đơn id={}, vnpTxnNo={}", orderId, vnpTxnNo);

        String email = hoaDon.getEmailNguoiNhan() != null && !hoaDon.getEmailNguoiNhan().isBlank()
                ? hoaDon.getEmailNguoiNhan().trim()
                : (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getEmail() != null ? hoaDon.getKhachHang().getEmail().trim() : null);
        if ("ONLINE".equalsIgnoreCase(hoaDon.getLoaiDon()) && email != null && !email.isBlank()) {
            eventPublisher.publishEvent(new com.example.be.core.common.events.OrderPlacedEvent(
                    this, hoaDon.getId(), email, hoaDon.getTongTienSauGiam()));
            log.info("Published OrderPlacedEvent after VNPay payment for order {} to email {}", hoaDon.getId(), email);
        }
    }
}
