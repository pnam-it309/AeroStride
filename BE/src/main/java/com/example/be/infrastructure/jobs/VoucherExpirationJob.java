package com.example.be.infrastructure.jobs;

import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VoucherExpirationJob
 * Quét bảo trì định kỳ 1 lần/ngày vào 2:15 AM để chuyển trạng thái phiếu giảm giá đã hết hạn
 * và tự động gửi email thông báo cho khách hàng sở hữu phiếu cá nhân đó.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherExpirationJob {

    private final AdminPhieuGiamGiaService adminPhieuGiamGiaService;

    @Scheduled(cron = "0 15 2 * * ?") // Chạy bảo trì lúc 2:15 sáng hàng ngày, không chạy khi startup
    public void execute() {
        log.info("Cron Job [VoucherExpirationJob] - Bắt đầu quét phiếu giảm giá hết hạn");
        try {
            adminPhieuGiamGiaService.checkAndExpireVouchers();
            log.info("Cron Job [VoucherExpirationJob] - Hoàn thành quét phiếu giảm giá hết hạn thành công");
        } catch (Exception e) {
            log.error("Cron Job [VoucherExpirationJob] - Thất bại với lỗi: {}", e.getMessage(), e);
        }
    }
}
