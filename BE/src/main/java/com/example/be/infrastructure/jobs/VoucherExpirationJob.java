package com.example.be.infrastructure.jobs;

import com.example.be.core.admin.phieugiamgia.service.AdminPhieuGiamGiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * VoucherExpirationJob
 * Quét định kỳ mỗi 5 phút để chuyển trạng thái các phiếu giảm giá đã hết hạn
 * và tự động gửi email thông báo cho khách hàng sở hữu phiếu cá nhân đó.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherExpirationJob {

    private final AdminPhieuGiamGiaService adminPhieuGiamGiaService;

    @Scheduled(cron = "0 */5 * * * *")
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
