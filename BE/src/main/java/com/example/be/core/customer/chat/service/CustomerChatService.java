package com.example.be.core.customer.chat.service;

import com.example.be.core.customer.chat.model.response.CustomerTinNhanResponse;
import java.util.List;

public interface CustomerChatService {
    List<CustomerTinNhanResponse> getMessagesBySessionId(String sessionId);
    void sendMessage(String conversationId, String text, String sender, String sessionId, String imageBase64);
    void submitRating(String sessionId, Integer rating, String feedback);
    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
