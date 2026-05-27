package com.example.be.core.admin.chat.service;

import com.example.be.entity.CuocHoiThoai;
import java.util.List;

public interface AiChatService {
    void generateAndSendResponse(CuocHoiThoai conversation, String customerText);
    List<String> getDynamicWelcomeSuggestions(String sessionId);
}
