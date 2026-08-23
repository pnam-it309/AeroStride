package com.example.be.core.admin.chat.service.impl;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.TinNhanResponse;
import com.example.be.core.admin.chat.repository.AdminCuocHoiThoaiRepository;
import com.example.be.core.admin.chat.repository.AdminTinNhanRepository;
import com.example.be.core.admin.chat.service.AdminChatService;
import com.example.be.infrastructure.constants.ChatConstants;
import com.example.be.infrastructure.constants.MessageConstants;
import com.example.be.infrastructure.constants.VaiTro;
import com.example.be.entity.CuocHoiThoai;
import com.example.be.entity.TinNhan;
import com.example.be.entity.NhanVien;
import com.example.be.repository.NhanVienRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminChatServiceImpl implements AdminChatService {

    private final AdminCuocHoiThoaiRepository conversationRepository;
    private final AdminTinNhanRepository messageRepository;
    private final NhanVienRepository nhanVienRepository;
    private final com.example.be.repository.KhachHangRepository khachHangRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.example.be.core.admin.chat.service.AiChatService aiChatService;
    private final com.example.be.infrastructure.security.service.UserPresenceService userPresenceService;

    @Value("${app.local-upload-dir}")
    private String localUploadDir;

    @Value("${app.base_url}")
    private String appBaseUrl;

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

        if (c.getMaPhien() != null && c.getMaPhien().startsWith("user_")) {
            String username = c.getMaPhien().substring(5);
            var kh = khachHangRepository.findByTenTaiKhoan(username)
                    .or(() -> khachHangRepository.findFirstByEmailIgnoreCase(username))
                    .orElse(null);
            if (kh != null && kh.getHinhAnh() != null && !kh.getHinhAnh().trim().isEmpty()) {
                return kh.getHinhAnh();
            }
        }
        
        if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
            NhanVien partner = null;
            if (c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername)) {
                partner = c.getNhanVienNhan();
            } else if (c.getNhanVienNhan() != null && c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername)) {
                partner = c.getNhanVien();
            } else if (c.getNhanVienNhan() != null) {
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
            String fullName = c.getKhachHang().getTen();
            if (fullName != null && !fullName.trim().isEmpty()) {
                return fullName.trim();
            }
            String username = c.getKhachHang().getTenTaiKhoan();
            if (username != null && !username.trim().isEmpty()) {
                return username.trim();
            }
            String email = c.getKhachHang().getEmail();
            if (email != null && !email.trim().isEmpty()) {
                return email.trim();
            }
            return "Khách hàng";
        }

        // Tự động giải mã tên khách hàng nếu mã phiên là user_
        if (c.getMaPhien() != null && c.getMaPhien().startsWith("user_")) {
            String username = c.getMaPhien().substring(5);
            var khOpt = khachHangRepository.findByTenTaiKhoan(username)
                    .or(() -> khachHangRepository.findFirstByEmailIgnoreCase(username));
            if (khOpt.isPresent()) {
                var kh = khOpt.get();
                c.setKhachHang(kh);
                conversationRepository.save(c);
                String fullName = kh.getTen();
                if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
                String uName = kh.getTenTaiKhoan();
                if (uName != null && !uName.trim().isEmpty()) return uName.trim();
                return kh.getEmail() != null ? kh.getEmail() : "Khách hàng";
            }
            return username;
        }
        
        if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
            boolean isSender = c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
            boolean isReceiver = c.getNhanVienNhan() != null && c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername);
            
            if (isSender && c.getNhanVienNhan() != null) {
                return c.getNhanVienNhan().getTen();
            } else if (isReceiver && c.getNhanVien() != null) {
                return c.getNhanVien().getTen();
            } else if (c.getNhanVien() != null && c.getNhanVienNhan() != null) {
                return c.getNhanVien().getTen() + " & " + c.getNhanVienNhan().getTen();
            } else if (c.getNhanVien() != null) {
                return c.getNhanVien().getTen();
            } else if (c.getNhanVienNhan() != null) {
                return c.getNhanVienNhan().getTen();
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
    public List<AdminChatResponse> getAllConversations(String type, String status, String keyword) {
        String currentUsername = getCurrentUsername();
        NhanVien currentNv = nhanVienRepository.findByTenTaiKhoan(currentUsername).orElse(null);
        boolean isManager = currentNv != null && VaiTro.isManagementRole(currentNv);
        
        List<AdminChatResponse> allConvs = conversationRepository.findAllWithDetails().stream()
                .filter(c -> {
                    // Logic lọc hội thoại:
                    // 1. Nếu là PENDING: Mọi nhân viên đều thấy để có thể tiếp nhận.
                    if (c.getTrangThaiHoiThoai() == CuocHoiThoai.TrangThaiHoiThoai.PENDING) {
                        return true;
                    }
                    
                    // 2. Nếu là CUSTOMER và đã ACTIVE hoặc CLOSED: Quản lý thấy tất cả, nhân viên thấy cuộc mình tiếp nhận.
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
                        if (isManager) {
                            return true;
                        }
                        return c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                    }
                    
                    // 3. Nếu là INTERNAL: Quản lý hoặc người gửi/người nhận mới thấy.
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
                        boolean isSender = c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername);
                        boolean isReceiver = c.getNhanVienNhan() != null && c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername);
                        return isManager || isSender || isReceiver;
                    }
                    
                    return false;
                })
                .map(c -> {
                    String partnerStaffId = null;
                    String partnerUsername = null;
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
                        if (c.getNhanVien() != null && c.getNhanVien().getTenTaiKhoan().equals(currentUsername)) {
                            if (c.getNhanVienNhan() != null) {
                                partnerStaffId = c.getNhanVienNhan().getId();
                                partnerUsername = c.getNhanVienNhan().getTenTaiKhoan();
                            }
                        } else if (c.getNhanVienNhan() != null && c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername)) {
                            if (c.getNhanVien() != null) {
                                partnerStaffId = c.getNhanVien().getId();
                                partnerUsername = c.getNhanVien().getTenTaiKhoan();
                            }
                        } else {
                            if (c.getNhanVienNhan() != null) {
                                partnerStaffId = c.getNhanVienNhan().getId();
                                partnerUsername = c.getNhanVienNhan().getTenTaiKhoan();
                            }
                        }
                    }

                    String role = "ROLE_NHAN_VIEN";
                    if (c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL) {
                        NhanVien partner = null;
                        if (c.getNhanVienNhan() != null && !c.getNhanVienNhan().getTenTaiKhoan().equals(currentUsername)) {
                            partner = c.getNhanVienNhan();
                        } else if (c.getNhanVien() != null) {
                            partner = c.getNhanVien();
                        }
                        if (partner != null && VaiTro.isManagementRole(partner)) {
                            role = "ROLE_QUAN_LY";
                        }
                    }
                    int unreadCount = 0;
                    if (c.getDanhSachTinNhan() != null && !c.getDanhSachTinNhan().isEmpty()) {
                        TinNhan last = c.getDanhSachTinNhan().get(c.getDanhSachTinNhan().size() - 1);
                        String lastSender = last.getLoaiNguoiGui();
                        if (lastSender != null && !lastSender.equalsIgnoreCase(currentUsername) && !"staff".equalsIgnoreCase(lastSender)) {
                            unreadCount = 1;
                        }
                    }
                    Long lastMsgTimestamp = c.getNgayCapNhat() != null ? c.getNgayCapNhat() : 0L;
                    if (c.getDanhSachTinNhan() != null && !c.getDanhSachTinNhan().isEmpty()) {
                        TinNhan last = c.getDanhSachTinNhan().get(c.getDanhSachTinNhan().size() - 1);
                        if (last.getNgayTao() != null) {
                            lastMsgTimestamp = last.getNgayTao();
                        }
                    }
                    boolean isPartnerOnline = partnerUsername != null && userPresenceService.isOnline(partnerUsername);
                    String internalStatus = isPartnerOnline ? CuocHoiThoai.TrangThaiHoiThoai.ACTIVE.name() : CuocHoiThoai.TrangThaiHoiThoai.CLOSED.name();

                    return AdminChatResponse.builder()
                            .id(c.getId())
                            .ten(getConversationName(c, currentUsername))
                            .tinNhanCuoi((c.getDanhSachTinNhan() == null || c.getDanhSachTinNhan().isEmpty()) ? "" : c.getDanhSachTinNhan().get(c.getDanhSachTinNhan().size() - 1).getNoiDung())
                            .anhDaiDien(getAvatarUrl(c, currentUsername))
                            .thoiGian(formatTime(c.getNgayCapNhat()))
                            .chuaDoc(0)
                            .daChapNhan(c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL ? true : c.getDaChapNhan())
                            .loaiHoiThoai(c.getLoaiHoiThoai() != null ? c.getLoaiHoiThoai().name() : CuocHoiThoai.LoaiHoiThoai.CUSTOMER.name())
                            .trangThaiHoiThoai(c.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.INTERNAL ? internalStatus : (c.getTrangThaiHoiThoai() != null ? c.getTrangThaiHoiThoai().name() : CuocHoiThoai.TrangThaiHoiThoai.PENDING.name()))
                            .idNhanVien(c.getNhanVien() != null ? c.getNhanVien().getId() : null)
                            .tenTaiKhoanNhanVien(c.getNhanVien() != null ? c.getNhanVien().getTenTaiKhoan() : null)
                            .idNhanVienNhan(c.getNhanVienNhan() != null ? c.getNhanVienNhan().getId() : null)
                            .tenTaiKhoanNhanVienNhan(c.getNhanVienNhan() != null ? c.getNhanVienNhan().getTenTaiKhoan() : null)
                            .idNhanVienDoiTac(partnerStaffId)
                            .tenTaiKhoanDoiTac(partnerUsername)
                            .chuaDoc(unreadCount)
                            .vaiTro(role)
                            .timestamp(lastMsgTimestamp)
                            .build();
                })
                .collect(Collectors.toCollection(ArrayList::new));

        // For INTERNAL chat, we want to see ALL staff except current user (as potential new chats)
        Set<String> existingInternalPartnerIds = allConvs.stream()
                .filter(c -> CuocHoiThoai.LoaiHoiThoai.INTERNAL.name().equals(c.getLoaiHoiThoai()))
                .map(AdminChatResponse::getIdNhanVienDoiTac)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<AdminChatResponse> allStaff = nhanVienRepository.findAll().stream()
                .filter(nv -> nv.getTenTaiKhoan() != null && !nv.getTenTaiKhoan().equals(currentUsername))
                .filter(nv -> !existingInternalPartnerIds.contains(nv.getId()))
                .map(nv -> AdminChatResponse.builder()
                        .id("NEW_INTERNAL_" + nv.getId())
                        .ten(nv.getTen())
                        .tinNhanCuoi("")
                        .anhDaiDien(nv.getHinhAnh() != null && !nv.getHinhAnh().trim().isEmpty() ? nv.getHinhAnh() : DEFAULT_AVATAR)
                        .thoiGian("")
                        .chuaDoc(0)
                        .daChapNhan(true)
                        .loaiHoiThoai(CuocHoiThoai.LoaiHoiThoai.INTERNAL.name())
                        .trangThaiHoiThoai(userPresenceService.isOnline(nv.getTenTaiKhoan()) ? CuocHoiThoai.TrangThaiHoiThoai.ACTIVE.name() : CuocHoiThoai.TrangThaiHoiThoai.CLOSED.name())
                        .idNhanVienDoiTac(nv.getId())
                        .tenTaiKhoanDoiTac(nv.getTenTaiKhoan())
                        .vaiTro(VaiTro.isManagementRole(nv) ? "ROLE_QUAN_LY" : "ROLE_NHAN_VIEN")
                        .timestamp(0L)
                        .build())
                .collect(Collectors.toList());
        
        allConvs.addAll(allStaff);

        // Apply filters
        return allConvs.stream()
                .filter(c -> type == null || type.isEmpty() || type.equals(c.getLoaiHoiThoai()))
                .filter(c -> status == null || status.isEmpty() || status.equals(c.getTrangThaiHoiThoai()))
                .filter(c -> keyword == null || keyword.isEmpty() || c.getTen().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getConversationStats() {
        return getConversationStats(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getConversationStats(String type) {
        List<AdminChatResponse> allConvs = getAllConversations(type, null, null);
        
        long activeCount = allConvs.stream().filter(c -> CuocHoiThoai.TrangThaiHoiThoai.ACTIVE.name().equals(c.getTrangThaiHoiThoai())).count();
        long pendingCount = allConvs.stream().filter(c -> CuocHoiThoai.TrangThaiHoiThoai.PENDING.name().equals(c.getTrangThaiHoiThoai())).count();
        long closedCount = allConvs.stream().filter(c -> CuocHoiThoai.TrangThaiHoiThoai.CLOSED.name().equals(c.getTrangThaiHoiThoai())).count();
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("ACTIVE", activeCount);
        stats.put("PENDING", pendingCount);
        stats.put("CLOSED", closedCount);
        return stats;
    }

    @Override
    @Transactional
    public List<TinNhanResponse> getMessagesByConversation(String id) {
        if (id.startsWith("NEW_INTERNAL_")) {
            return List.of(); 
        }
        messageRepository.markAllAsReadByConversationId(id);
        return messageRepository.findByCuocHoiThoai_IdOrderByNgayTaoAsc(id).stream()
                .map(m -> TinNhanResponse.builder()
                        .id(m.getId())
                        .idCuocHoiThoai(m.getCuocHoiThoai().getId())
                        .maPhien(m.getCuocHoiThoai().getMaPhien())
                        .idNhanVien(m.getCuocHoiThoai().getNhanVien() != null ? m.getCuocHoiThoai().getNhanVien().getId() : null)
                        .tenTaiKhoanNhanVien(m.getCuocHoiThoai().getNhanVien() != null ? m.getCuocHoiThoai().getNhanVien().getTenTaiKhoan() : null)
                        .idNhanVienNhan(m.getCuocHoiThoai().getNhanVienNhan() != null ? m.getCuocHoiThoai().getNhanVienNhan().getId() : null)
                        .tenTaiKhoanNhanVienNhan(m.getCuocHoiThoai().getNhanVienNhan() != null ? m.getCuocHoiThoai().getNhanVienNhan().getTenTaiKhoan() : null)
                        .nguoiGui(m.getLoaiNguoiGui())
                        .noiDung(m.getNoiDung())
                        .hinhAnh(m.getHinhAnh())
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
                        .hinhAnh(m.getHinhAnh())
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
            conversation.setNgayCapNhat(System.currentTimeMillis());
            
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

        // Nếu là nhân viên gửi, tự động tiếp nhận cuộc trò chuyện (nếu đang chờ), chặn nếu đã đóng
        if (ChatConstants.SENDER_TYPE_STAFF.equals(senderType) && conversation.getLoaiHoiThoai() == CuocHoiThoai.LoaiHoiThoai.CUSTOMER) {
            if (conversation.getTrangThaiHoiThoai() == CuocHoiThoai.TrangThaiHoiThoai.CLOSED) {
                throw new RuntimeException("Phiên trò chuyện đã đóng, không thể gửi thêm tin nhắn.");
            }
            if (Boolean.FALSE.equals(conversation.getDaChapNhan()) || conversation.getTrangThaiHoiThoai() != CuocHoiThoai.TrangThaiHoiThoai.ACTIVE) {
                conversation.setDaChapNhan(true);
                conversation.setTrangThaiHoiThoai(CuocHoiThoai.TrangThaiHoiThoai.ACTIVE);
                String staffUsername = SecurityContextHolder.getContext().getAuthentication().getName();
                if (staffUsername != null && !staffUsername.isBlank()) {
                    nhanVienRepository.findByTenTaiKhoan(staffUsername).ifPresent(conversation::setNhanVien);
                }
                conversation = conversationRepository.save(conversation);
            }
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

        TinNhanResponse response = TinNhanResponse.builder()
                .id(savedMessage.getId())
                .idCuocHoiThoai(conversation.getId())
                .maPhien(conversation.getMaPhien())
                .idNhanVien(conversation.getNhanVien() != null ? conversation.getNhanVien().getId() : null)
                .tenTaiKhoanNhanVien(conversation.getNhanVien() != null ? conversation.getNhanVien().getTenTaiKhoan() : null)
                .idNhanVienNhan(conversation.getNhanVienNhan() != null ? conversation.getNhanVienNhan().getId() : null)
                .tenTaiKhoanNhanVienNhan(conversation.getNhanVienNhan() != null ? conversation.getNhanVienNhan().getTenTaiKhoan() : null)
                .nguoiGui(senderType)
                .noiDung(text)
                .hinhAnh(imageUrl)
                .thoiGian(formatTime(savedMessage.getNgayTao()))
                .build();

        messagingTemplate.convertAndSend(ChatConstants.TOPIC_MESSAGES, response);

        // Phát thông báo cho tất cả client để đồng bộ danh sách hội thoại và tin nhắn mới
        Map<String, String> notification = new HashMap<>();
        notification.put("content", "NEW_MESSAGE");
        notification.put("conversationId", conversation.getId());
        notification.put("timestamp", Instant.now().toString());
        publishNotification(notification);

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

    @Override
    @Transactional
    public boolean markAsRead(String id) {
        if (id == null || id.startsWith("NEW_INTERNAL_")) {
            return false;
        }
        messageRepository.markAllAsReadByConversationId(id);

        Map<String, String> notification = new HashMap<>();
        notification.put("content", "READ_CONVERSATION_" + id);
        notification.put("timestamp", Instant.now().toString());
        publishNotification(notification);

        return true;
    }

    private void publishNotification(Map<String, String> notification) {
        try {
            redisTemplate.convertAndSend(ChatConstants.REDIS_CHANNEL_NOTIFICATIONS, notification);
        } catch (DataAccessException ex) {
            log.warn("Redis notification publish failed; continuing without cross-instance fanout. Error: {}", ex.getMessage());
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
    @Transactional(readOnly = true)
    public String summarizeConversation(String id) {
        CuocHoiThoai conversation = conversationRepository.findById(id).orElse(null);
        if (conversation == null) {
            return "Không tìm thấy cuộc hội thoại.";
        }
        return aiChatService.summarizeChat(conversation);
    }
}

