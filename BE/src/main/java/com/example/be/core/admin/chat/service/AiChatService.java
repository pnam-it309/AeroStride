package com.example.be.core.admin.chat.service;

import com.example.be.entity.ChatConversation;
import java.util.List;

public interface AiChatService {
    void generateAndSendResponse(ChatConversation conversation, String customerText);
    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
