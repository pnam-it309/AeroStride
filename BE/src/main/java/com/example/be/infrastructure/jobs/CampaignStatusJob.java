package com.example.be.infrastructure.jobs;

import com.example.be.entity.DotGiamGia;
import com.example.be.infrastructure.constants.TrangThai;
import com.example.be.repository.DotGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CampaignStatusJob
 * Quét định kỳ mỗi 5 phút để kích hoạt hoặc kết thúc các đợt giảm giá theo thời gian thực.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignStatusJob {

    private final DotGiamGiaRepository dotGiamGiaRepository;

    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // Chạy mỗi 5 phút, bắt đầu sau 1 phút khởi động để tránh quá tải
    @Transactional
    public void execute() {
        long now = System.currentTimeMillis();
        log.info("Cron Job [CampaignStatusJob] - Bắt đầu đồng bộ trạng thái đợt giảm giá");
        
        try {
            // 1. Kích hoạt đợt giảm giá sắp diễn ra -> Đang hoạt động
            Specification<DotGiamGia> toActivateSpec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("trangThai"), TrangThai.NGUNG_HOAT_DONG),
                    cb.lessThanOrEqualTo(root.get("ngayBatDau"), now),
                    cb.greaterThanOrEqualTo(root.get("ngayKetThuc"), now)
            );
            List<DotGiamGia> toActivate = dotGiamGiaRepository.findAll(toActivateSpec);
            if (!toActivate.isEmpty()) {
                for (DotGiamGia d : toActivate) {
                    d.setTrangThai(TrangThai.DANG_HOAT_DONG);
                }
                dotGiamGiaRepository.saveAll(toActivate);
                log.info("CampaignStatusJob - Đã kích hoạt hoạt động {} đợt giảm giá mới", toActivate.size());
            }

            // 2. Chuyển sang kết thúc cho các đợt đã quá hạn -> Không hoạt động
            Specification<DotGiamGia> toExpireSpec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("trangThai"), TrangThai.DANG_HOAT_DONG),
                    cb.lessThan(root.get("ngayKetThuc"), now)
            );
            List<DotGiamGia> toExpire = dotGiamGiaRepository.findAll(toExpireSpec);
            if (!toExpire.isEmpty()) {
                for (DotGiamGia d : toExpire) {
                    d.setTrangThai(TrangThai.NGUNG_HOAT_DONG);
                }
                dotGiamGiaRepository.saveAll(toExpire);
                log.info("CampaignStatusJob - Đã đóng {} đợt giảm giá hết hạn", toExpire.size());
            }

            log.info("Cron Job [CampaignStatusJob] - Đồng bộ hoàn thành thành công");
        } catch (Exception e) {
            log.error("Cron Job [CampaignStatusJob] - Thất bại với lỗi: {}", e.getMessage(), e);
        }
    }
}
