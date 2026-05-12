package com.example.be.core.admin.chat.controller;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.ChatMessageResponse;
import com.example.be.core.admin.chat.service.AdminChatService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.ADMIN_CHAT)
@RequiredArgsConstructor
public class AdminChatController {

    private final AdminChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations() {
        List<AdminChatResponse> data = chatService.getAllConversations();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String id) {
        List<ChatMessageResponse> data = chatService.getMessagesByConversation(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations/{id}/accept")
    public ResponseEntity<?> acceptConversation(@PathVariable String id) {
        boolean success = chatService.acceptConversation(id, "ADMIN");
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        if (success) {
            Map<String, String> data = new HashMap<>();
            data.put("staffCode", "ADMIN");
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> payload) {
        String conversationId = payload.get("conversationId");
        String text = payload.get("text");
        String sender = payload.get("sender");
        
        chatService.sendMessage(conversationId, text, sender, null);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
