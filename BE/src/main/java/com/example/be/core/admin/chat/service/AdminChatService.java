package com.example.be.core.admin.chat.service;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.TinNhanResponse;

import java.util.List;
import java.util.Map;

public interface AdminChatService {

    List<AdminChatResponse> getAllConversations(String type, String status, String keyword);
    Map<String, Long> getConversationStats();

    List<TinNhanResponse> getMessagesByConversation(String id);

    List<TinNhanResponse> getMessagesBySessionId(String sessionId);

    boolean acceptConversation(String id);

    boolean closeConversation(String id);

    boolean deleteConversation(String id);

    void sendMessage(String conversationId, String text, String senderType, String sessionId);

    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
