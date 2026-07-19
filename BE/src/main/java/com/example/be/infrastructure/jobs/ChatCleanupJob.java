package com.example.be.infrastructure.jobs;

import com.example.be.entity.CuocHoiThoai;
import com.example.be.repository.CuocHoiThoaiRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCleanupJob {

    private final CuocHoiThoaiRepository cuocHoiThoaiRepository;

    /**
     * Runs every 5 minutes to close active chats that have been inactive for 15 minutes.
     * Cron: "0 0/5 * * * *"
     */
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void execute() {
        log.info("Cron Job [ChatCleanupJob] - Started closing inactive chats");
        try {
            long fifteenMinutesAgo = System.currentTimeMillis() - (15L * 60 * 1000L);
            
            List<CuocHoiThoai> inactiveChats = cuocHoiThoaiRepository.findAll().stream()
                    .filter(c -> c.getTrangThaiHoiThoai() == CuocHoiThoai.TrangThaiHoiThoai.ACTIVE)
                    .filter(c -> c.getNgayCapNhat() != null && c.getNgayCapNhat() < fifteenMinutesAgo)
                    .toList();

            for (CuocHoiThoai chat : inactiveChats) {
                chat.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.CLOSED);
                chat.setNgayCapNhat(System.currentTimeMillis());
                cuocHoiThoaiRepository.save(chat);
            }

            log.info("Cron Job [ChatCleanupJob] - Finished. Closed {} inactive chats.", inactiveChats.size());
        } catch (Exception e) {
            log.error("Cron Job [ChatCleanupJob] - Failed with error: {}", e.getMessage());
        }
    }
}
