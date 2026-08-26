package com.example.be.core.admin.chat.scheduler;

import com.example.be.core.admin.chat.repository.AdminCuocHoiThoaiRepository;
import com.example.be.core.admin.chat.repository.AdminTinNhanRepository;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.infrastructure.constants.ChatConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ScheduledChatTasks {

    @Autowired
    private AdminCuocHoiThoaiRepository cuocHoiThoaiRepository;

    @Autowired
    private AdminTinNhanRepository tinNhanRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Run every 5 minutes: auto-close inactive chats after 30 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCloseInactiveChats() {
        // Find all ACTIVE and PENDING conversations
        List<CuocHoiThoai> activeChats = cuocHoiThoaiRepository.findByTrangThaiHoiThoaiIn(
                List.of(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE, CuocHoiThoai.TrangThaiHoiThoai.PENDING));

        long now = System.currentTimeMillis();

        for (CuocHoiThoai chat : activeChats) {
            // CHỈ áp dụng đóng phiên tự động cho khách hàng (CUSTOMER)
            // Tuyệt đối KHÔNG áp dụng cho nhân viên nội bộ (INTERNAL)
            if (chat.getLoaiHoiThoai() != CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
                continue;
            }

            // Find the last message time
            Optional<TinNhan> lastMsg = tinNhanRepository.findTopByCuocHoiThoaiOrderByNgayTaoDesc(chat);
            
            Long lastActivity = chat.getNgayCapNhat() != null ? chat.getNgayCapNhat() : chat.getNgayTao();
            if (lastMsg.isPresent() && lastMsg.get().getNgayTao() != null) {
                lastActivity = lastMsg.get().getNgayTao();
            }

            if (lastActivity != null) {
                long minutesSinceLastMsg = (now - lastActivity) / (60 * 1000);
                // If no activity in the last 30 minutes, close the chat
                if (minutesSinceLastMsg >= 30) {
                    chat.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.CLOSED);
                    chat.setNgayCapNhat(now);
                    
                    // Add a system message notifying auto-close
                    TinNhan systemMsg = TinNhan.builder()
                            .cuocHoiThoai(chat)
                            .loaiNguoiGui("system")
                            .tenNguoiGui("SYSTEM")
                            .noiDung("Cuộc trò chuyện đã tự động kết thúc do không có phản hồi trong thời gian dài.")
                            .build();
                    
                    tinNhanRepository.save(systemMsg);
                    cuocHoiThoaiRepository.save(chat);

                    Map<String, String> notification = new HashMap<>();
                    notification.put("content", "CLOSED_CONVERSATION_" + chat.getId());
                    notification.put("timestamp", String.valueOf(now));
                    messagingTemplate.convertAndSend(ChatConstants.TOPIC_NOTIFICATIONS, notification);
                }
            }
        }
    }

    // Run every 30 seconds: auto-delete closed guest conversations after 2 minutes
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cleanupClosedGuestChats() {
        long now = System.currentTimeMillis();
        long twoMinutesInMillis = 2 * 60 * 1000L; // 2 minutes

        // Find all CLOSED conversations
        List<CuocHoiThoai> closedChats = cuocHoiThoaiRepository.findByTrangThaiHoiThoaiIn(
                List.of(CuocHoiThoai.TrangThaiHoiThoai.CLOSED));

        for (CuocHoiThoai chat : closedChats) {
            // Check if guest customer (khachHang is null, loaiHoiThoai is CUSTOMER)
            if (chat.getKhachHang() == null && chat.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
                Long closedTime = chat.getNgayCapNhat() != null ? chat.getNgayCapNhat() : chat.getNgayTao();
                if (closedTime != null && (now - closedTime) >= twoMinutesInMillis) {
                    String chatId = chat.getId();
                    String session = chat.getMaPhien();
                    log.info("Auto deleting closed guest conversation: {} (session: {})", chatId, session);
                    try {
                        tinNhanRepository.deleteByConversationId(chatId);
                        cuocHoiThoaiRepository.delete(chat);

                        // Broadcast notification so admin UI removes this conversation
                        Map<String, Object> deletedEvent = new HashMap<>();
                        deletedEvent.put("type", "CONVERSATION_DELETED");
                        deletedEvent.put("conversationId", chatId);
                        deletedEvent.put("sessionId", session);
                        messagingTemplate.convertAndSend(ChatConstants.TOPIC_NOTIFICATIONS, deletedEvent);
                    } catch (Exception e) {
                        log.error("Error deleting closed guest chat {}: {}", chatId, e.getMessage());
                    }
                }
            }
        }
    }
}
