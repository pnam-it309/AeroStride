package com.example.be.core.customer.chat.controller;

import com.example.be.core.admin.chat.service.AdminChatService;
import com.example.be.infrastructure.constants.RoutesConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(RoutesConstant.CUSTOMER_CHAT)
@RequiredArgsConstructor
public class CustomerChatController {

    private final AdminChatService chatService; // Reusing service for simplicity

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> payload) {
        String conversationId = payload.get("conversationId");
        String sessionId = payload.get("sessionId");
        String text = payload.get("text");
        String sender = payload.get("sender");
        
        chatService.sendMessage(conversationId, text, sender, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", chatService.getMessagesBySessionId(sessionId));
        return ResponseEntity.ok(response);
    }
}
