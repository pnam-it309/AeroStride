package com.example.be.core.admin.chat.service;

import com.example.be.core.admin.chat.model.AdminChatResponse;
import com.example.be.core.admin.chat.model.ChatMessageResponse;

import java.util.List;

public interface AdminChatService {

    List<AdminChatResponse> getAllConversations();

    List<ChatMessageResponse> getMessagesByConversation(String id);

    List<ChatMessageResponse> getMessagesBySessionId(String sessionId);

    boolean acceptConversation(String id);

    boolean closeConversation(String id);

    void sendMessage(String conversationId, String text, String senderType, String sessionId);

    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
