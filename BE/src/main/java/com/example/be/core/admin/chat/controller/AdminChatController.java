package com.example.be.core.admin.chat.controller;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.TinNhanResponse;
import com.example.be.core.admin.chat.model.SendMessageRequest;
import com.example.be.core.admin.chat.service.AdminChatService;
import com.example.be.core.common.dto.ApiResponse;
import com.example.be.infrastructure.constants.RoutesConstant;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping(RoutesConstant.ADMIN_CHAT)
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('QUAN_TRI_VIEN', 'NHAN_VIEN')")
public class AdminChatController {

    private final AdminChatService chatService;

    @GetMapping(RoutesConstant.CONVERSATIONS)
    public ResponseEntity<ApiResponse<List<AdminChatResponse>>> getConversations() {
        return ResponseEntity.ok(ApiResponse.success(chatService.getAllConversations()));
    }

    @GetMapping(RoutesConstant.CONVERSATION_MESSAGES)
    public ResponseEntity<ApiResponse<List<TinNhanResponse>>> getDanhSachTinNhan(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(chatService.getMessagesByConversation(id)));
    }

    @PostMapping(RoutesConstant.CONVERSATION_ACCEPT)
    public ResponseEntity<ApiResponse<Map<String, String>>> acceptConversation(@PathVariable String id) {
        boolean success = chatService.acceptConversation(id);
        if (success) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            Map<String, String> data = Map.of("staffCode", currentUsername);
            return ResponseEntity.ok(ApiResponse.success(data));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không thể chấp nhận cuộc hội thoại"));
    }

    @PostMapping(RoutesConstant.CONVERSATION_CLOSE)
    public ResponseEntity<ApiResponse<Void>> closeConversation(@PathVariable String id) {
        boolean success = chatService.closeConversation(id);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Không thể đóng cuộc hội thoại"));
    }

    @PostMapping(RoutesConstant.SEND)
    public ResponseEntity<ApiResponse<Void>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        chatService.sendMessage(request.getConversationId(), request.getText(), request.getSender(), null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
