package com.example.be.core.admin.chat.scheduler;

import com.example.be.core.admin.chat.repository.AdminCuocHoiThoaiRepository;
import com.example.be.core.admin.chat.repository.AdminTinNhanRepository;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class ScheduledChatTasks {

    @Autowired
    private AdminCuocHoiThoaiRepository cuocHoiThoaiRepository;

    @Autowired
    private AdminTinNhanRepository tinNhanRepository;

    // Run every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCloseInactiveChats() {
        // Find all ACTIVE and PENDING conversations
        List<CuocHoiThoai> activeChats = cuocHoiThoaiRepository.findByTrangThaiHoiThoaiIn(
                List.of(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE, CuocHoiThoai.TrangThaiHoiThoai.PENDING));

        LocalDateTime now = LocalDateTime.now();

        for (CuocHoiThoai chat : activeChats) {
            // Find the last message time
            Optional<TinNhan> lastMsg = tinNhanRepository.findTopByCuocHoiThoaiOrderByNgayTaoDesc(chat);
            
            LocalDateTime lastActivity = chat.getNgaySua(); // default fallback
            if (lastMsg.isPresent()) {
                lastActivity = lastMsg.get().getNgayTao();
            }

            if (lastActivity != null) {
                long minutesSinceLastMsg = ChronoUnit.MINUTES.between(lastActivity, now);
                // If no activity in the last 30 minutes, close the chat
                if (minutesSinceLastMsg >= 30) {
                    chat.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.CLOSED);
                    
                    // Add a system message notifying auto-close
                    TinNhan systemMsg = TinNhan.builder()
                            .cuocHoiThoai(chat)
                            .nguoiGui("SYSTEM")
                            .noiDung("Cuộc trò chuyện đã tự động kết thúc do không có phản hồi trong thời gian dài.")
                            .build();
                    
                    tinNhanRepository.save(systemMsg);
                    cuocHoiThoaiRepository.save(chat);
                }
            }
        }
    }
}
