package com.example.be.core.admin.chat.service.impl;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.TinNhanResponse;
import com.example.be.core.admin.chat.repository.AdminCuocHoiThoaiRepository;
import com.example.be.core.admin.chat.repository.AdminTinNhanRepository;
import com.example.be.core.admin.chat.service.AdminChatService;
import com.example.be.infrastructure.constants.ChatConstants;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.entity.NhanVien;
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
public class AdminChatServiceImpl implements AdminChatService {

    private final AdminCuocHoiThoaiRepository conversationRepository;
    private final AdminTinNhanRepository messageRepository;
    private final NhanVienRepository nhanVienRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.example.be.core.admin.chat.service.AiChatService aiChatService;

    private static final String DEFAULT_AVATAR = "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png";

    private String formatTime(Long timestamp) {
        if (timestamp == null) return "Vừa xong";
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String getAvatarUrl(CuocHoiThoai c, String currentUsername) {
        if (c.getKhachHang() != null) {
            String hinhAnh = c.getKhachHang().getHinhAnh();
            return (hinhAnh != null && !hinhAnh.trim().isEmpty()) ? hinhAnh : DEFAULT_AVATAR;
        }
        
        if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
            NhanVien partner = null;
            if (c.getNhanVienNhan() != null && !c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername)) {
                partner = c.getNhanVienNhan();
            } else if (c.getNhanVien() != null) {
                partner = c.getNhanVien();
            }
            if (partner != null) {
                String hinhAnh = partner.getHinhAnh();
                return (hinhAnh != null && !hinhAnh.trim().isEmpty()) ? hinhAnh : DEFAULT_AVATAR;
            }
        }
        
        return DEFAULT_AVATAR;
    }

    private String getConversationName(CuocHoiThoai c, String currentUsername) {
        if (c.getKhachHang() != null) {
            return c.getKhachHang().getTenTaiKhoan();
        }
        
        if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
            if (c.getNhanVienNhan() != null && !c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername)) {
                return c.getNhanVienNhan().getTen();
            }
            if (c.getNhanVien() != null) {
                return c.getNhanVien().getTen();
            }
            return ChatConstants.DEFAULT_STAFF_NAME;
        }
        
        // Khách vãng lai: Ghép 4 ký tự cuối của sessionId
        String sessionId = c.getMaPhien();
        if (sessionId != null && sessionId.length() > 4) {
            String shortId = sessionId.substring(sessionId.length() - 4);
            return ChatConstants.DEFAULT_CUSTOMER_NAME + " #" + shortId.toUpperCase();
        }
        
        // Fallback dùng 4 ký tự cuối của Conversation ID
        if (c.getId() != null && c.getId().length() > 4) {
            String shortId = c.getId().substring(c.getId().length() - 4);
            return ChatConstants.DEFAULT_CUSTOMER_NAME + " #" + shortId.toUpperCase();
        }
        
        return ChatConstants.DEFAULT_CUSTOMER_NAME;
    }

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "ADMIN";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminChatResponse> getAllConversations() {
        String currentUsername = getCurrentUsername();
        
        List<AdminChatResponse> allConvs = conversationRepository.findAll().stream()
                .filter(c -> {
                    // Logic lọc hội thoại:
                    // 1. Nếu là PENDING: Mọi nhân viên đều thấy để có thể tiếp nhận.
                    if (c.getTrangThaiHoiThoai() == CuocHoiThoai.TrangThaiHoiThoai.PENDING) {
                        return true;
                    }
                    
                    // 2. Nếu là CUSTOMER và đã ACTIVE hoặc CLOSED: Chỉ người tiếp nhận mới thấy.
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
                        return c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                    }
                    
                    // 3. Nếu là INTERNAL: Chỉ người gửi hoặc người nhận mới thấy.
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
                        boolean isSender = c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                        boolean isReceiver = c.getNhanVienNhan() != null && c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername);
                        return isSender || isReceiver;
                    }
                    
                    return false;
                })
                .map(c -> AdminChatResponse.builder()
                        .id(c.getId())
                        .ten(getConversationName(c, currentUsername))
                        .tinNhanCuoi(c.getDanhSachTinNhan().isEmpty() ? "" : c.getDanhSachTinNhan().get(c.getDanhSachTinNhan().size() - 1).getNoiDung())
                        .anhDaiDien(getAvatarUrl(c, currentUsername))
                        .thoiGian(formatTime(c.getNgayCapNhat()))
                        .chuaDoc(0)
                        .daChapNhan(c.getDaChapNhan())
                        .loaiHoiThoai(c.getLoaiHoiThoai() != null ? c.getLoaiHoiThoai().name() : CuocHoiThoai.LoaiHoiThoai.CUSTOMER.name())
                        .trangThaiHoiThoai(c.getTrangThaiHoiThoai() != null ? c.getTrangThaiHoiThoai().name() : CuocHoiThoai.TrangThaiHoiThoai.PENDING.name())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));

        // For INTERNAL chat, we want to see ALL staff except current user (as potential new chats)
        List<AdminChatResponse> allStaff = nhanVienRepository.findAll().stream()
                .filter(nv -> !nv.getTenTaiKhoan().equals(currentUsername))
                .filter(nv -> allConvs.stream().noneMatch(c -> CuocHoiThoai.LoaiHoiThoai.INTERNAL.name().equals(c.getLoaiHoiThoai()) && c.getTen().equals(nv.getTen())))
                .map(nv -> AdminChatResponse.builder()
                        .id("NEW_INTERNAL_" + nv.getId())
                        .ten(nv.getTen())
                        .tinNhanCuoi("")
                        .anhDaiDien(nv.getHinhAnh() != null && !nv.getHinhAnh().trim().isEmpty() ? nv.getHinhAnh() : DEFAULT_AVATAR)
                        .thoiGian("")
                        .chuaDoc(0)
                        .daChapNhan(true)
                        .loaiHoiThoai(CuocHoiThoai.LoaiHoiThoai.INTERNAL.name())
                        .trangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE.name())
                        .build())
                .collect(Collectors.toList());
        
        allConvs.addAll(allStaff);
        return allConvs;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TinNhanResponse> getMessagesByConversation(String id) {
        if (id.startsWith("NEW_INTERNAL_")) {
            return List.of(); 
        }
        return messageRepository.findByCuocHoiThoai_IdOrderByNgayTaoAsc(id).stream()
                .map(m -> TinNhanResponse.builder()
                        .id(m.getId())
                        .idCuocHoiThoai(m.getCuocHoiThoai().getId())
                        .maPhien(m.getCuocHoiThoai().getMaPhien())
                        .nguoiGui(m.getLoaiNguoiGui())
                        .noiDung(m.getNoiDung())
                        .thoiGian(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TinNhanResponse> getMessagesBySessionId(String sessionId) {
        return messageRepository.findByCuocHoiThoai_MaPhienOrderByNgayTaoAsc(sessionId).stream()
                .map(m -> TinNhanResponse.builder()
                        .id(m.getId())
                        .idCuocHoiThoai(m.getCuocHoiThoai().getId())
                        .maPhien(m.getCuocHoiThoai().getMaPhien())
                        .nguoiGui(m.getLoaiNguoiGui())
                        .noiDung(m.getNoiDung())
                        .thoiGian(formatTime(m.getNgayTao()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean acceptConversation(String id) {
        CuocHoiThoai conversation = conversationRepository.findById(id).orElse(null);
        if (conversation != null) {
            conversation.setDaChapNhan(true);
            conversation.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE);
            
            String currentUsername = getCurrentUsername();
            Optional<NhanVien> nhanVienOpt = nhanVienRepository.findByTenTaiKhoan(currentUsername);
            
            if (nhanVienOpt.isPresent()) {
                NhanVien nv = nhanVienOpt.get();
                conversation.setNhanVien(nv);
                
                conversation = conversationRepository.save(conversation);
                
                String systemMessageText = "Nhân viên " + nv.getMa() + " đã tiếp nhận cuộc trò chuyện.";
                
                TinNhan systemMessage = TinNhan.builder()
                        .cuocHoiThoai(conversation)
                        .loaiNguoiGui(ChatConstants.SENDER_TYPE_SYSTEM)
                        .noiDung(systemMessageText)
                        .build();
                
                TinNhan savedMessage = messageRepository.save(systemMessage);
                
                TinNhanResponse response = TinNhanResponse.builder()
                        .id(savedMessage.getId())
                        .idCuocHoiThoai(conversation.getId())
                        .maPhien(conversation.getMaPhien())
                        .idNhanVien(nv.getTenTaiKhoan())
                        .nguoiGui(ChatConstants.SENDER_TYPE_SYSTEM)
                        .noiDung(systemMessageText)
                        .thoiGian(formatTime(savedMessage.getNgayTao()))
                        .build();
                
                messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);
            } else {
                conversationRepository.save(conversation);
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean closeConversation(String id) {
        CuocHoiThoai conversation = conversationRepository.findById(id).orElse(null);
        if (conversation != null) {
            conversation.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.CLOSED);
            
            String currentUsername = getCurrentUsername();
            Optional<NhanVien> nhanVienOpt = nhanVienRepository.findByTenTaiKhoan(currentUsername);
            String operatorName = nhanVienOpt.isPresent() ? nhanVienOpt.get().getMa() : "Nhân viên";
            
            conversation = conversationRepository.save(conversation);
            
            String systemMessageText = "Cuộc trò chuyện đã được đóng bởi nhân viên " + operatorName + ".";
            
            TinNhan systemMessage = TinNhan.builder()
                    .cuocHoiThoai(conversation)
                    .loaiNguoiGui(ChatConstants.SENDER_TYPE_SYSTEM)
                    .noiDung(systemMessageText)
                    .build();
            
            TinNhan savedMessage = messageRepository.save(systemMessage);
            
            TinNhanResponse response = TinNhanResponse.builder()
                    .id(savedMessage.getId())
                    .idCuocHoiThoai(conversation.getId())
                    .maPhien(conversation.getMaPhien())
                    .idNhanVien(currentUsername)
                    .nguoiGui(ChatConstants.SENDER_TYPE_SYSTEM)
                    .noiDung(systemMessageText)
                    .thoiGian(formatTime(savedMessage.getNgayTao()))
                    .build();
            
            messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);
            
            Map<String, String> notification = new HashMap<>();
            notification.put("content", "CLOSED_CONVERSATION_" + id);
            notification.put("timestamp", Instant.now().toString());
            publishNotification(notification);

            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean deleteConversation(String id) {
        CuocHoiThoai conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null) {
            return false;
        }

        // Xóa toàn bộ tin nhắn trước rồi xóa cuộc hội thoại (xóa hẳn lịch sử)
        messageRepository.deleteByConversationIdIn(List.of(id));
        conversationRepository.deleteById(id);

        // Báo cho các client khác refresh lại danh sách
        Map<String, String> notification = new HashMap<>();
        notification.put("content", "DELETED_CONVERSATION_" + id);
        notification.put("timestamp", Instant.now().toString());
        publishNotification(notification);

        return true;
    }

    @Override
    @Transactional
    public void sendMessage(String conversationId, String text, String senderType, String sessionId) {
        CuocHoiThoai conversation;

        if (conversationId != null && conversationId.startsWith("NEW_INTERNAL_")) {
            String targetStaffId = conversationId.replace("NEW_INTERNAL_", "");
            String currentUsername = getCurrentUsername();
            NhanVien sender = nhanVienRepository.findByTenTaiKhoan(currentUsername)
                    .orElseThrow(() -> new RuntimeException(ChatConstants.ERR_SENDER_NOT_FOUND));
            
            // Check if conversation already exists
            Optional<CuocHoiThoai> existing = conversationRepository.findInternalConversation(sender.getId(), targetStaffId);
            
            if (existing.isPresent()) {
                conversation = existing.get();
            } else {
                conversation = CuocHoiThoai.builder()
                        .loaiHoiThoai(CuocHoiThoai.LoaiHoiThoai.INTERNAL)
                        .trangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE)
                        .daChapNhan(true)
                        .nhanVien(sender)
                        .nhanVienNhan(nhanVienRepository.findById(targetStaffId).orElseThrow(() -> new RuntimeException(ChatConstants.ERR_SENDER_NOT_FOUND)))
                        .build();
                conversation = conversationRepository.save(conversation);
            }
        } else if (conversationId != null && !conversationId.isEmpty() && !conversationId.equals("undefined")) {
            conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException(ChatConstants.ERR_CONVERSATION_NOT_FOUND));
        } else if (sessionId != null && !sessionId.isEmpty()) {
            boolean isNew = conversationRepository.findByMaPhien(sessionId).isEmpty();
            conversation = conversationRepository.findByMaPhien(sessionId)
                    .orElseGet(() -> {
                        CuocHoiThoai newConv = CuocHoiThoai.builder()
                                .maPhien(sessionId)
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

        TinNhan message = TinNhan.builder()
                .cuocHoiThoai(conversation)
                .loaiNguoiGui(senderType)
                .noiDung(text)
                .build();

        TinNhan savedMessage = messageRepository.save(message);

        TinNhanResponse response = TinNhanResponse.builder()
                .id(savedMessage.getId())
                .idCuocHoiThoai(conversation.getId())
                .maPhien(conversation.getMaPhien())
                .idNhanVien(conversation.getNhanVien() != null ? conversation.getNhanVien().getTenTaiKhoan() : null)
                .idNhanVienNhan(conversation.getNhanVienNhan() != null ? conversation.getNhanVienNhan().getTenTaiKhoan() : null)
                .nguoiGui(senderType)
                .noiDung(text)
                .thoiGian(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);

        log.info("Checking AI Trigger: senderType={}, isAccepted={}, convType={}", 
                senderType, conversation.getDaChapNhan(), conversation.getLoaiHoiThoai());

        // AI Chatbot logic: Nếu khách hàng gửi tin và chưa có nhân viên tiếp nhận
        if (ChatConstants.SENDER_TYPE_CUSTOMER.equals(senderType) && 
            Boolean.FALSE.equals(conversation.getDaChapNhan()) && 
            (conversation.getLoaiHoiThoai() == null || conversation.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER)) {
            log.info("Triggering AI response for conversation: {}", conversation.getId());
            aiChatService.generateAndSendResponse(conversation, text);
        }
    }

    private void publishNotification(Map<String, String> notification) {
        try {
            redisTemplate.convertAndSend(ChatConstants.REDIS_CHANNEL_NOTIFICATIONS, notification);
        } catch (DataAccessException ex) {
            log.warn("Redis notification publish failed; continuing without cross-instance fanout. Error: {}", ex.getMessage());
        }
    }

    @Override
    public List<String> getDynamicWelcomeSuggestions(String sessionId) {
        return aiChatService.getDynamicWelcomeSuggestions(sessionId);
    }
}

