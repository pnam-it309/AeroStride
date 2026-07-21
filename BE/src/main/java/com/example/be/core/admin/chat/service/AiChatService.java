package com.example.be.core.admin.chat.service;

import com.example.be.entity.CuocHoiThoai;
import java.util.List;

public interface AiChatService {
    void generateAndSendResponse(CuocHoiThoai conversation, String customerText, String imageBase64);
    default void generateAndSendResponse(CuocHoiThoai conversation, String customerText) {
        generateAndSendResponse(conversation, customerText, null);
    }
    String summarizeChat(CuocHoiThoai conversation);
    List<String> getDynamicWelcomeSuggestions(String sessionId);
    String getDashboardInsights(int pendingOrders, int lowStockItems);
}

