package com.example.be.core.customer.chat.controller;

import com.example.be.core.customer.chat.model.request.CustomerChatRequest;
import com.example.be.core.customer.chat.service.CustomerChatService;
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

    private final CustomerChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody CustomerChatRequest payload) {
        chatService.sendMessage(payload.getConversationId(), payload.getText(), payload.getSender(), payload.getSessionId());
        
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

    @GetMapping("/welcome-suggestions")
    public ResponseEntity<?> getWelcomeSuggestions(@RequestParam String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", chatService.getDynamicWelcomeSuggestions(sessionId));
        return ResponseEntity.ok(response);
    }
}
