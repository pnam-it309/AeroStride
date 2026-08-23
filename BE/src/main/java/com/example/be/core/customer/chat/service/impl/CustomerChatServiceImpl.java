package com.example.be.core.customer.chat.service.impl;

import com.example.be.core.customer.chat.model.response.CustomerTinNhanResponse;
import com.example.be.core.customer.chat.repository.CustomerCuocHoiThoaiRepository;
import com.example.be.core.customer.chat.repository.CustomerTinNhanRepository;
import com.example.be.core.customer.chat.service.CustomerChatService;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.infrastructure.constants.ChatConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerChatServiceImpl implements CustomerChatService {

    private final CustomerCuocHoiThoaiRepository conversationRepository;
    private final CustomerTinNhanRepository messageRepository;
    private final com.example.be.repository.KhachHangRepository khachHangRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.example.be.core.admin.chat.service.AiChatService aiChatService;

    @Value("${app.local-upload-dir}")
    private String localUploadDir;

    @Value("${app.base_url}")
    private String appBaseUrl;

    private com.example.be.entity.KhachHang resolveCustomer(String sessionId) {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
                String principalName = auth.getName();
                var kh = khachHangRepository.findByTenTaiKhoan(principalName);
                if (kh.isPresent()) return kh.get();
                kh = khachHangRepository.findFirstByEmailIgnoreCase(principalName);
                if (kh.isPresent()) return kh.get();
            }
        } catch (Exception ignored) {}

        if (sessionId != null && sessionId.startsWith("user_")) {
            String username = sessionId.substring(5);
            var kh = khachHangRepository.findByTenTaiKhoan(username);
            if (kh.isPresent()) return kh.get();
            kh = khachHangRepository.findFirstByEmailIgnoreCase(username);
            if (kh.isPresent()) return kh.get();
        }
        return null;
    }

    private String formatTime(Long timestamp) {
        if (timestamp == null) return "Vừa xong";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerTinNhanResponse> getMessagesBySessionId(String sessionId) {
        return messageRepository.findByCuocHoiThoai_MaPhienOrderByNgayTaoAsc(sessionId).stream()
                .map(m -> CustomerTinNhanResponse.builder()
                        .id(m.getId())
                        .idCuocHoiThoai(m.getCuocHoiThoai().getId())
                        .maPhien(m.getCuocHoiThoai().getMaPhien())
                        .nguoiGui(m.getLoaiNguoiGui())
                        .noiDung(m.getNoiDung())
                        .hinhAnh(m.getHinhAnh())
                        .thoiGian(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    /** Lưu ảnh base64 xuống disk và trả về URL. Trả null nếu rỗng hoặc lỗi. */
    /**
     * Chuẩn hóa ảnh chat sang data URI để lưu trữ bền vững trong database (LONGTEXT).
     * Đảm bảo không bị mất ảnh khi F5, mở tab mới, hay khi container redeploy.
     */
    private String processChatImage(String imageBase64) {
        if (!StringUtils.hasText(imageBase64)) return null;
        if (imageBase64.startsWith("data:image/") || imageBase64.startsWith("http://") || imageBase64.startsWith("https://") || imageBase64.startsWith("/uploads/")) {
            return imageBase64;
        }
        return "data:image/jpeg;base64," + imageBase64;
    }

    @Override
    @Transactional
    public void sendMessage(String conversationId, String text, String senderType, String sessionId, String imageBase64) {
        CuocHoiThoai conversation;
        com.example.be.entity.KhachHang currentCustomer = resolveCustomer(sessionId);

        if (conversationId != null && !conversationId.isEmpty() && !conversationId.equals("undefined")) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException(ChatConstants.ERR_CONVERSATION_NOT_FOUND));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            boolean isNew = conversationRepository.findByMaPhien(sessionId).isEmpty();
            conversation = conversationRepository.findByMaPhien(sessionId)
                    .orElseGet(() -> {
                        CuocHoiThoai newConv = CuocHoiThoai.builder()
                                .maPhien(sessionId)
                                .khachHang(currentCustomer)
                                .daChapNhan(false)
                                .trangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.PENDING)
                                .build();
                        return conversationRepository.save(newConv);
                    });

            if (isNew) {
                Map<String, String> notification = new HashMap<>();
                notification.put("content", ChatConstants.MSG_NEW_CUSTOMER_WAITING);
                notification.put("timestamp", Instant.now().toString());
                publishNotification(notification);
            }
        } else {
            throw new RuntimeException("Phải có Conversation ID hoặc Session ID");
        }

        // Tự động gắn Khách Hàng vào cuộc hội thoại nếu chưa có
        if (conversation.getKhachHang() == null && currentCustomer != null) {
            conversation.setKhachHang(currentCustomer);
            conversation = conversationRepository.save(conversation);
        }

        // Nếu khách hàng gửi tin nhắn vào cuộc trò chuyện đã đóng, tự động mở lại
        if (ChatConstants.SENDER_TYPE_CUSTOMER.equals(senderType) && conversation.getTrangThaiHoiThoai() == CuocHoiThoai.TrangThaiHoiThoai.CLOSED) {
            log.info("Khách hàng gửi tin nhắn mới vào phiên đã đóng. Tự động mở lại cuộc trò chuyện.");
            conversation.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.PENDING);
            conversation.setDaChapNhan(false);
            conversation.setNhanVien(null);
            conversation = conversationRepository.save(conversation);
        }

        // Kiểm tra nếu là nhân viên gửi thì cuộc trò chuyện phải được tiếp nhận
        if (ChatConstants.SENDER_TYPE_STAFF.equals(senderType) && Boolean.FALSE.equals(conversation.getDaChapNhan()) && conversation.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
            throw new RuntimeException(ChatConstants.ERR_CONVERSATION_NOT_ACCEPTED);
        }

        // Xử lý ảnh: lưu data URI bền vững vào database
        String imageUrl = processChatImage(imageBase64);

        TinNhan message = TinNhan.builder()
                .cuocHoiThoai(conversation)
                .loaiNguoiGui(senderType)
                .noiDung(text)
                .hinhAnh(imageUrl)
                .build();

        TinNhan savedMessage = messageRepository.save(message);

        CustomerTinNhanResponse response = CustomerTinNhanResponse.builder()
                .id(savedMessage.getId())
                .idCuocHoiThoai(conversation.getId())
                .maPhien(conversation.getMaPhien())
                .idNhanVien(conversation.getNhanVien() != null ? conversation.getNhanVien().getTenTaiKhoan() : null)
                .idNhanVienNhan(conversation.getNhanVienNhan() != null ? conversation.getNhanVienNhan().getTenTaiKhoan() : null)
                .nguoiGui(senderType)
                .noiDung(text)
                .hinhAnh(imageUrl)
                .thoiGian(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);

        Map<String, String> notification = new HashMap<>();
        notification.put("content", "NEW_MESSAGE_" + conversation.getId());
        notification.put("timestamp", Instant.now().toString());
        publishNotification(notification);

        log.info("Checking AI Trigger: senderType={}, isAccepted={}, convType={}", 
                senderType, conversation.getDaChapNhan(), conversation.getLoaiHoiThoai());

        // AI Chatbot logic: Nếu khách hàng gửi tin và chưa có nhân viên tiếp nhận
        if (ChatConstants.SENDER_TYPE_CUSTOMER.equals(senderType) && 
            !Boolean.TRUE.equals(conversation.getDaChapNhan()) && 
            (conversation.getLoaiHoiThoai() == null || conversation.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER)) {
            log.info("Triggering AI response for conversation: {}", conversation.getId());
            aiChatService.generateAndSendResponse(conversation, text, imageBase64);
        }
    }

    private void publishNotification(Map<String, String> notification) {
        try {
            redisTemplate.convertAndSend(ChatConstants.REDIS_CHANNEL_NOTIFICATIONS, notification);
        } catch (DataAccessException ex) {
            log.warn("Redis notification publish failed; continuing. Error: {}", ex.getMessage());
        }
        try {
            messagingTemplate.convertAndSend(ChatConstants.TOPIC_NOTIFICATIONS, notification);
        } catch (Exception ex) {
            log.warn("Local STOMP notification broadcast failed: {}", ex.getMessage());
        }
    }

    @Override
    public List<String> getDynamicWelcomeSuggestions(String sessionId) {
        return aiChatService.getDynamicWelcomeSuggestions(sessionId);
    }

    @Override
    @Transactional
    public void submitRating(String sessionId, Integer rating, String feedback) {
        CuocHoiThoai conversation = conversationRepository.findByMaPhien(sessionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cuộc trò chuyện."));
        
        conversation.setDanhGiaChat(rating);
        conversation.setPhanHoiChat(feedback);
        conversationRepository.save(conversation);
    }
}
