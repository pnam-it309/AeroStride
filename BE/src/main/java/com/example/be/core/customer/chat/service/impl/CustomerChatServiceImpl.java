package com.example.be.core.customer.chat.service.impl;

import com.example.be.core.customer.chat.model.response.CustomerChatMessageResponse;
import com.example.be.core.customer.chat.repository.CustomerChatConversationRepository;
import com.example.be.core.customer.chat.repository.CustomerChatMessageRepository;
import com.example.be.core.customer.chat.service.CustomerChatService;
import com.example.be.entity.ChatConversation;
import com.example.be.entity.ChatMessage;
import com.example.be.infrastructure.constants.ChatConstants;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerChatServiceImpl implements CustomerChatService {

    private final CustomerChatConversationRepository conversationRepository;
    private final CustomerChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.example.be.core.admin.chat.service.AiChatService aiChatService;

    private String formatTime(Long timestamp) {
        if (timestamp == null) return "Vừa xong";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerChatMessageResponse> getMessagesBySessionId(String sessionId) {
        return messageRepository.findByConversation_SessionIdOrderByNgayTaoAsc(sessionId).stream()
                .map(m -> CustomerChatMessageResponse.builder()
                        .id(m.getId())
                        .conversationId(m.getConversation().getId())
                        .sessionId(m.getConversation().getSessionId())
                        .sender(m.getSenderType())
                        .text(m.getContent())
                        .time(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void sendMessage(String conversationId, String text, String senderType, String sessionId) {
        ChatConversation conversation;

        if (conversationId != null && !conversationId.isEmpty() && !conversationId.equals("undefined")) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException(ChatConstants.ERR_CONVERSATION_NOT_FOUND));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            boolean isNew = conversationRepository.findBySessionId(sessionId).isEmpty();
            conversation = conversationRepository.findBySessionId(sessionId)
                    .orElseGet(() -> {
                        ChatConversation newConv = ChatConversation.builder()
                                .sessionId(sessionId)
                                .isAccepted(false)
                                .status(ChatConversation.ChatStatus.PENDING)
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

        // Nếu khách hàng gửi tin nhắn vào cuộc trò chuyện đã đóng, tự động mở lại
        if (ChatConstants.SENDER_TYPE_CUSTOMER.equals(senderType) && conversation.getStatus() == ChatConversation.ChatStatus.CLOSED) {
            log.info("Khách hàng gửi tin nhắn mới vào phiên đã đóng. Tự động mở lại cuộc trò chuyện.");
            conversation.setStatus(ChatConversation.ChatStatus.PENDING);
            conversation.setIsAccepted(false);
            conversation.setNhanVien(null);
            conversation = conversationRepository.save(conversation);
        }

        // Kiểm tra nếu là nhân viên gửi thì cuộc trò chuyện phải được tiếp nhận
        if (ChatConstants.SENDER_TYPE_STAFF.equals(senderType) && Boolean.FALSE.equals(conversation.getIsAccepted()) && conversation.getType() == ChatConversation.ChatType.CUSTOMER) {
            throw new RuntimeException(ChatConstants.ERR_CONVERSATION_NOT_ACCEPTED);
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(text)
                .build();

        ChatMessage savedMessage = messageRepository.save(message);

        CustomerChatMessageResponse response = CustomerChatMessageResponse.builder()
                .id(savedMessage.getId())
                .conversationId(conversation.getId())
                .sessionId(conversation.getSessionId())
                .staffId(conversation.getNhanVien() != null ? conversation.getNhanVien().getTenTaiKhoan() : null)
                .secondStaffId(conversation.getSecondNhanVien() != null ? conversation.getSecondNhanVien().getTenTaiKhoan() : null)
                .sender(senderType)
                .text(text)
                .time(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);

        log.info("Checking AI Trigger: senderType={}, isAccepted={}, convType={}", 
                senderType, conversation.getIsAccepted(), conversation.getType());

        // AI Chatbot logic: Nếu khách hàng gửi tin và chưa có nhân viên tiếp nhận
        if (ChatConstants.SENDER_TYPE_CUSTOMER.equals(senderType) && 
            Boolean.FALSE.equals(conversation.getIsAccepted()) && 
            (conversation.getType() == null || conversation.getType() == ChatConversation.ChatType.CUSTOMER)) {
            log.info("Triggering AI response for conversation: {}", conversation.getId());
            aiChatService.generateAndSendResponse(conversation, text);
        }
    }

    private void publishNotification(Map<String, String> notification) {
        try {
            redisTemplate.convertAndSend(ChatConstants.REDIS_CHANNEL_NOTIFICATIONS, notification);
        } catch (DataAccessException ex) {
            log.warn("Redis notification publish failed; continuing. Error: {}", ex.getMessage());
        }
    }

    @Override
    public List<String> getDynamicWelcomeSuggestions(String sessionId) {
        return aiChatService.getDynamicWelcomeSuggestions(sessionId);
    }
}
