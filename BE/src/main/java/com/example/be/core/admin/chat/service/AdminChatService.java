package com.example.be.core.admin.chat.service;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.ChatMessageResponse;
import com.example.be.entity.ChatConversation;
import com.example.be.entity.ChatMessage;
import com.example.be.entity.NhanVien;
import com.example.be.repository.ChatConversationRepository;
import com.example.be.repository.ChatMessageRepository;
import com.example.be.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "ADMIN";
        }
    }

    @Transactional(readOnly = true)
    public List<AdminChatResponse> getAllConversations() {
        String currentUsername = getCurrentUsername();
        
        List<AdminChatResponse> allConvs = conversationRepository.findAll().stream()
                .filter(c -> {
                    // Logic lọc hội thoại:
                    // 1. Nếu là PENDING: Mọi nhân viên đều thấy để có thể tiếp nhận.
                    if (c.getStatus() == ChatConversation.ChatStatus.PENDING) {
                        return true;
                    }
                    
                    // 2. Nếu là CUSTOMER và đã ACTIVE hoặc CLOSED: Chỉ người tiếp nhận mới thấy.
                    if (c.getType() == ChatConversation.ChatType.CUSTOMER) {
                        return c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                    }
                    
                    // 3. Nếu là INTERNAL: Chỉ người gửi hoặc người nhận mới thấy.
                    if (c.getType() == ChatConversation.ChatType.INTERNAL) {
                        boolean isSender = c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                        boolean isReceiver = c.getSecondNhanVien() != null && c.getSecondNhanVien().getTenTaiKhoan().equals(currentUsername);
                        return isSender || isReceiver;
                    }
                    
                    return false;
                })
                .map(c -> AdminChatResponse.builder()
                        .id(c.getId())
                        .name(c.getKhachHang() != null ? c.getKhachHang().getTenTaiKhoan() : 
                              (c.getType() == ChatConversation.ChatType.INTERNAL ? 
                               (c.getSecondNhanVien() != null && !c.getSecondNhanVien().getTenTaiKhoan().equals(currentUsername) ? c.getSecondNhanVien().getTen() : 
                                (c.getNhanVien() != null ? c.getNhanVien().getTen() : "Nhân viên")) : "Khách vãng lai"))
                        .lastMsg(c.getMessages().isEmpty() ? "" : c.getMessages().get(c.getMessages().size() - 1).getContent())
                        .avatar(c.getKhachHang() != null ? c.getKhachHang().getTenTaiKhoan().substring(0, 1) : 
                               (c.getType() == ChatConversation.ChatType.INTERNAL ? "NV" : "K"))
                        .time(formatTime(c.getNgayCapNhat()))
                        .unread(0)
                        .isAccepted(c.getIsAccepted())
                        .type(c.getType() != null ? c.getType().name() : "CUSTOMER")
                        .status(c.getStatus() != null ? c.getStatus().name() : "PENDING")
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        // For INTERNAL chat, we want to see ALL staff except current user (as potential new chats)
        List<AdminChatResponse> allStaff = nhanVienRepository.findAll().stream()
                .filter(nv -> !nv.getTenTaiKhoan().equals(currentUsername))
                .filter(nv -> allConvs.stream().noneMatch(c -> "INTERNAL".equals(c.getType()) && c.getName().equals(nv.getTen())))
                .map(nv -> AdminChatResponse.builder()
                        .id("NEW_INTERNAL_" + nv.getId())
                        .name(nv.getTen())
                        .lastMsg("")
                        .avatar("NV")
                        .time("")
                        .unread(0)
                        .isAccepted(true)
                        .type("INTERNAL")
                        .status("ACTIVE")
                        .build())
                .collect(Collectors.toList());
        
        allConvs.addAll(allStaff);
        return allConvs;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesByConversation(String id) {
        if (id.startsWith("NEW_INTERNAL_")) {
            return List.of(); 
        }
        return messageRepository.findByConversationIdOrderByNgayTaoAsc(id).stream()
                .map(m -> ChatMessageResponse.builder()
                        .id(m.getId())
                        .conversationId(m.getConversation().getId())
                        .sessionId(m.getConversation().getSessionId())
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
                        .sessionId(m.getConversation().getSessionId())
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
            conversation.setStatus(ChatConversation.ChatStatus.ACTIVE);
            nhanVienRepository.findById(staffId).ifPresent(conversation::setNhanVien);
            conversationRepository.save(conversation);
            return true;
        }
        return false;
    }

    @Transactional
    public void sendMessage(String conversationId, String text, String senderType, String sessionId) {
        ChatConversation conversation;

        if (conversationId != null && conversationId.startsWith("NEW_INTERNAL_")) {
            String targetStaffId = conversationId.replace("NEW_INTERNAL_", "");
            String currentUsername = getCurrentUsername();
            NhanVien sender = nhanVienRepository.findByTenTaiKhoan(currentUsername)
                    .orElseThrow(() -> new RuntimeException("Sender staff not found"));
            
            // Check if conversation already exists
            Optional<ChatConversation> existing = conversationRepository.findInternalConversation(sender.getId(), targetStaffId);
            
            if (existing.isPresent()) {
                conversation = existing.get();
            } else {
                conversation = ChatConversation.builder()
                        .type(ChatConversation.ChatType.INTERNAL)
                        .status(ChatConversation.ChatStatus.ACTIVE)
                        .isAccepted(true)
                        .nhanVien(sender)
                        .secondNhanVien(nhanVienRepository.findById(targetStaffId).orElseThrow(() -> new RuntimeException("Target staff not found")))
                        .build();
                conversation = conversationRepository.save(conversation);
            }
        } else if (conversationId != null && !conversationId.isEmpty() && !conversationId.equals("undefined")) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
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
                notification.put("content", "Có khách vãng lai mới đang chờ hỗ trợ!");
                notification.put("timestamp", Instant.now().toString());
                publishNotification(notification);
            }
        } else {
            throw new RuntimeException("Phải có Conversation ID hoặc Session ID");
        }

        // Kiểm tra nếu là nhân viên gửi thì cuộc trò chuyện phải được tiếp nhận
        if ("staff".equals(senderType) && Boolean.FALSE.equals(conversation.getIsAccepted()) && conversation.getType() == ChatConversation.ChatType.CUSTOMER) {
            throw new RuntimeException("Cuộc trò chuyện chưa được tiếp nhận");
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(text)
                .build();

        ChatMessage savedMessage = messageRepository.save(message);

        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(savedMessage.getId())
                .conversationId(conversation.getId())
                .sessionId(conversation.getSessionId())
                .staffId(conversation.getNhanVien() != null ? conversation.getNhanVien().getTenTaiKhoan() : null)
                .secondStaffId(conversation.getSecondNhanVien() != null ? conversation.getSecondNhanVien().getTenTaiKhoan() : null)
                .sender(senderType)
                .text(text)
                .time(formatTime(savedMessage.getNgayTao()))
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
