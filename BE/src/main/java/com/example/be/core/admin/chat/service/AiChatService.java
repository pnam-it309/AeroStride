package com.example.be.core.admin.chat.service;

import com.example.be.entity.ChatConversation;

public interface AiChatService {
    void generateAndSendResponse(ChatConversation conversation, String customerText);
}
