package com.example.be.infrastructure.listener;

import com.example.be.core.common.events.OrderPlacedEvent;
import com.example.be.core.notification.EmailService;
import com.example.be.core.notification.dto.EmailRequest;
import com.example.be.entity.HoaDon;
import com.example.be.entity.HoaDonChiTiet;
import com.example.be.repository.HoaDonChiTietRepository;
import com.example.be.repository.HoaDonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listener for system-wide notifications.
 * Decouples the business process (like ordering) from the notification process (like email).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService emailService;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;

    /**
     * Listens for OrderPlacedEvent and sends a confirmation email.
     * TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) ensures
     * the email is only sent if the database transaction is successfully committed.
     * @Async makes the email sending non-blocking for the main request thread.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent for order: {}. Sending confirmation to {}",
                event.getOrderId(), event.getUserEmail());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("orderId", event.getOrderId());
            variables.put("totalAmount", event.getTotalAmount());

            HoaDon hoaDon = hoaDonRepository.findById(event.getOrderId()).orElse(null);
            String orderCode = event.getOrderId();
            if (hoaDon != null) {
                if (hoaDon.getMaHoaDon() != null) orderCode = hoaDon.getMaHoaDon();
                variables.put("orderCode", orderCode);
                variables.put("tenNguoiNhan", hoaDon.getTenNguoiNhan());
                variables.put("soDienThoai", hoaDon.getSoDienThoaiNguoiNhan());
                variables.put("diaChi", hoaDon.getDiaChiNguoiNhan());
                variables.put("tamtinh", hoaDon.getTongTien());
                variables.put("phiVanChuyen", hoaDon.getPhiVanChuyen());
                variables.put("tongTienSauGiam", hoaDon.getTongTienSauGiam());

                BigDecimal tienGiam = BigDecimal.ZERO;
                if (hoaDon.getTongTien() != null && hoaDon.getPhiVanChuyen() != null && hoaDon.getTongTienSauGiam() != null) {
                    tienGiam = hoaDon.getTongTien().add(hoaDon.getPhiVanChuyen()).subtract(hoaDon.getTongTienSauGiam());
                    if (tienGiam.compareTo(BigDecimal.ZERO) < 0) tienGiam = BigDecimal.ZERO;
                }
                variables.put("tienGiam", tienGiam);

                String phuongThuc = "COD";
                if (hoaDon.getListsGiaoDichThanhToan() != null && !hoaDon.getListsGiaoDichThanhToan().isEmpty()) {
                    var gd = hoaDon.getListsGiaoDichThanhToan().iterator().next();
                    if (gd.getPhuongThucThanhToan() != null && gd.getPhuongThucThanhToan().getTen() != null) {
                        phuongThuc = gd.getPhuongThucThanhToan().getTen();
                    } else if (gd.getLoaiGiaoDich() != null) {
                        phuongThuc = gd.getLoaiGiaoDich();
                    }
                }
                variables.put("phuongThuc", phuongThuc);

                List<Map<String, Object>> itemMaps = new ArrayList<>();
                List<HoaDonChiTiet> hdcts = hoaDonChiTietRepository.findAllByHoaDon(hoaDon);
                if (hdcts != null) {
                    for (HoaDonChiTiet hdct : hdcts) {
                        Map<String, Object> itemMap = new HashMap<>();
                        var ctsp = hdct.getChiTietSanPham();
                        String tenSP = "Sản phẩm";
                        String phanLoai = "";
                        if (ctsp != null) {
                            if (ctsp.getSanPham() != null) {
                                tenSP = ctsp.getSanPham().getTen();
                            }
                            String mau = ctsp.getMauSac() != null ? ctsp.getMauSac().getTen() : "";
                            String size = ctsp.getKichThuoc() != null ? ctsp.getKichThuoc().getTen() : "";
                            phanLoai = mau + (size.isEmpty() ? "" : " / Size " + size);
                        }
                        itemMap.put("tenSanPham", tenSP);
                        itemMap.put("phanLoai", phanLoai);
                        itemMap.put("soLuong", hdct.getSoLuong());
                        itemMap.put("donGia", hdct.getDonGia());
                        itemMap.put("thanhTien", hdct.getDonGia() != null && hdct.getSoLuong() != null 
                                ? hdct.getDonGia().multiply(BigDecimal.valueOf(hdct.getSoLuong())) 
                                : BigDecimal.ZERO);
                        itemMaps.add(itemMap);
                    }
                }
                variables.put("items", itemMaps);
            }

            EmailRequest request = EmailRequest.builder()
                    .to(event.getUserEmail())
                    .subject("AeroStride - Xác nhận đặt hàng #" + orderCode)
                    .templateName("order-confirmation")
                    .variables(variables)
                    .build();

            emailService.sendHtmlEmail(request);
            log.info("Order confirmation email sent successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}",
                    event.getOrderId(), e.getMessage());
        }
    }
}
