package com.example.be.core.admin.hoadon.job;

import com.example.be.entity.HoaDon;
import com.example.be.entity.LichSuTrangThaiHoaDon;
import com.example.be.infrastructure.constants.OrderStatus;
import com.example.be.repository.HoaDonRepository;
import com.example.be.repository.LichSuTrangThaiHoaDonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAutoCompleteJob {

    private final HoaDonRepository hoaDonRepository;
    private final LichSuTrangThaiHoaDonRepository lichSuTrangThaiHoaDonRepository;

    // Run every day at 00:00:00
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoCompleteOrders() {
        log.info("Starting OrderAutoCompleteJob to check for DANG_GIAO orders...");
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        
        List<HoaDon> orders = hoaDonRepository.findAll().stream()
                .filter(hd -> hd.getTrangThai() == OrderStatus.DANG_GIAO)
                .toList();

        for (HoaDon order : orders) {
            // Find when it was updated to DANG_GIAO
            List<LichSuTrangThaiHoaDon> histories = lichSuTrangThaiHoaDonRepository.findAllByHoaDonOrderByNgayTaoDesc(order);
            boolean shouldComplete = false;

            if (histories != null && !histories.isEmpty()) {
                LichSuTrangThaiHoaDon lastStatus = histories.get(0);
                if (lastStatus.getTrangThaiMoi() != null && lastStatus.getTrangThaiMoi() == OrderStatus.DANG_GIAO.ordinal()) {
                    if (lastStatus.getNgayTao() != null && lastStatus.getNgayTao() < sevenDaysAgo) {
                        shouldComplete = true;
                    }
                }
            } else {
                // If no history, just check order creation time (fallback)
                if (order.getNgayTao() != null && order.getNgayTao() < sevenDaysAgo) {
                    shouldComplete = true;
                }
            }

            if (shouldComplete) {
                log.info("Auto completing order: {}", order.getMaHoaDon());
                int oldStatus = order.getTrangThai().ordinal();
                order.setTrangThai(OrderStatus.HOAN_THANH);
                hoaDonRepository.save(order);

                LichSuTrangThaiHoaDon history = LichSuTrangThaiHoaDon.builder()
                        .hoaDon(order)
                        .trangThaiCu(oldStatus)
                        .trangThaiMoi(OrderStatus.HOAN_THANH.ordinal())
                        .ghiChu("Tự động hoàn thành do không có phản hồi sau 7 ngày")
                        .nguoiThucHien("Hệ thống")
                        .build();
                history.setNgayTao(System.currentTimeMillis());
                lichSuTrangThaiHoaDonRepository.save(history);
            }
        }
        
        log.info("Finished OrderAutoCompleteJob.");
    }
}
