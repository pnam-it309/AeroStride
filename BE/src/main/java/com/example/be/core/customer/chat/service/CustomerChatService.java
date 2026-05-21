package com.example.be.core.customer.chat.service;

import com.example.be.core.customer.chat.model.response.CustomerChatMessageResponse;
import java.util.List;

public interface CustomerChatService {
    List<CustomerChatMessageResponse> getMessagesBySessionId(String sessionId);
    void sendMessage(String conversationId, String text, String sender, String sessionId);
    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
