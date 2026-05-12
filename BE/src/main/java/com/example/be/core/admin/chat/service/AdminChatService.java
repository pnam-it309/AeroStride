package com.example.be.core.admin.chat.service;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.ChatMessageResponse;
import com.example.be.entity.ChatConversation;
import com.example.be.entity.ChatMessage;
import com.example.be.repository.ChatConversationRepository;
import com.example.be.repository.ChatMessageRepository;
import com.example.be.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final NhanVienRepository nhanVienRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private String formatTime(Long timestamp) {
        if (timestamp == null) return "Vừa xong";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Transactional(readOnly = true)
    public List<AdminChatResponse> getAllConversations() {
        return conversationRepository.findAll().stream()
                .map(c -> AdminChatResponse.builder()
                        .id(c.getId())
                        .name(c.getKhachHang() != null ? c.getKhachHang().getTenTaiKhoan() : "Khách vãng lai")
                        .lastMsg(c.getMessages().isEmpty() ? "" : c.getMessages().get(c.getMessages().size() - 1).getContent())
                        .avatar(c.getKhachHang() != null ? c.getKhachHang().getTenTaiKhoan().substring(0, 1) : "K")
                        .time(formatTime(c.getNgayCapNhat()))
                        .unread(0)
                        .isAccepted(c.getIsAccepted())
                        .type(c.getType() != null ? c.getType().name() : "CUSTOMER")
                        .status(c.getStatus() != null ? c.getStatus().name() : "PENDING")
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesByConversation(String id) {
        return messageRepository.findByConversationIdOrderByNgayTaoAsc(id).stream()
                .map(m -> ChatMessageResponse.builder()
                        .id(m.getId())
                        .conversationId(m.getConversation().getId())
                        .sender(m.getSenderType())
                        .text(m.getContent())
                        .time(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesBySessionId(String sessionId) {
        return messageRepository.findByConversation_SessionIdOrderByNgayTaoAsc(sessionId).stream()
                .map(m -> ChatMessageResponse.builder()
                        .id(m.getId())
                        .conversationId(m.getConversation().getId())
                        .sender(m.getSenderType())
                        .text(m.getContent())
                        .time(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean acceptConversation(String id, String staffId) {
        ChatConversation conversation = conversationRepository.findById(id).orElse(null);
        if (conversation != null) {
            conversation.setIsAccepted(true);
            nhanVienRepository.findById(staffId).ifPresent(conversation::setNhanVien);
            conversationRepository.save(conversation);
            return true;
        }
        return false;
    }

    @Transactional
    public void sendMessage(String conversationId, String text, String senderType, String sessionId) {
        ChatConversation conversation;

        if (conversationId != null && !conversationId.isEmpty() && !conversationId.equals("undefined")) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            boolean isNew = conversationRepository.findBySessionId(sessionId).isEmpty();
            conversation = conversationRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        ChatConversation newConv = ChatConversation.builder()
                                .sessionId(sessionId)
                                .isAccepted(false)
                                .build();
                        return conversationRepository.save(newConv);
                    });

            if (isNew) {
                Map<String, String> notification = new HashMap<>();
                notification.put("content", "Có khách vãng lai mới đang chờ hỗ trợ!");
                notification.put("timestamp", Instant.now().toString());
                publishNotification(notification);
            }
        } else {
            throw new RuntimeException("Phải có Conversation ID hoặc Session ID");
        }

        // Kiểm tra nếu là nhân viên gửi thì cuộc trò chuyện phải được tiếp nhận
        if ("staff".equals(senderType) && Boolean.FALSE.equals(conversation.getIsAccepted())) {
            throw new RuntimeException("Cuộc trò chuyện chưa được tiếp nhận");
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(text)
                .build();

        messageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(conversation.getId())
                .sender(senderType)
                .text(text)
                .time(formatTime(message.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend("/topic/messages", response);
    }

    private void publishNotification(Map<String, String> notification) {
        try {
            redisTemplate.convertAndSend("notifications", notification);
        } catch (DataAccessException ex) {
            log.warn("Redis notification publish failed; continuing without cross-instance fanout. Error: {}", ex.getMessage());
        }
    }
}
