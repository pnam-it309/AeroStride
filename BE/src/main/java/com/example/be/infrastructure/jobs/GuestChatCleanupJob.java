package com.example.be.infrastructure.jobs;

import com.example.be.repository.CuocHoiThoaiRepository;
import com.example.be.repository.TinNhanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Dọn dẹp lịch sử chat của khách vãng lai (chưa đăng nhập) không hoạt động quá thời gian cấu hình.
 * - Chỉ xóa hội thoại có maPhien và khachHang IS NULL. Hội thoại đã gắn tài khoản đăng nhập được giữ lại.
 * - Mốc "hoạt động" = thời điểm tin nhắn mới nhất (đồng hồ reset mỗi khi có tin nhắn mới).
 */
@Component
@Slf4j
public class GuestChatCleanupJob {

    private final CuocHoiThoaiRepository conversationRepository;
    private final TinNhanRepository messageRepository;

    @Value("${guest.chat.cleanup.threshold-ms:1800000}")
    private long thresholdMs;

    // Dùng @Qualifier để chọn đúng repository gốc (admin/customer cũng kế thừa cùng kiểu nên cần chỉ định).
    public GuestChatCleanupJob(
            @Qualifier("cuocHoiThoaiRepository") CuocHoiThoaiRepository conversationRepository,
            @Qualifier("tinNhanRepository") TinNhanRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Scheduled(fixedDelayString = "${guest.chat.cleanup.interval-ms:300000}", initialDelay = 60000)
    @Transactional
    public void execute() {
        long cutoff = System.currentTimeMillis() - thresholdMs;
        List<String> staleIds = conversationRepository.findStaleGuestConversationIds(cutoff);
        if (staleIds.isEmpty()) {
            return;
        }
        messageRepository.deleteByConversationIdIn(staleIds);
        conversationRepository.deleteByIdIn(staleIds);
        log.info("Cron Job [GuestChatCleanupJob] - Đã xóa {} hội thoại khách vãng lai không hoạt động > {} phút",
                staleIds.size(), thresholdMs / 60000);
    }
}
