package com.example.be.core.admin.chat.repository;

import com.example.be.entity.ChatMessage;
import com.example.be.repository.ChatMessageRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminChatMessageRepository extends ChatMessageRepository, JpaSpecificationExecutor<ChatMessage> {
    List<ChatMessage> findByConversationIdOrderByNgayTaoAsc(String conversationId);
    List<ChatMessage> findByConversation_SessionIdOrderByNgayTaoAsc(String sessionId);

    // Lấy N tin nhắn gần nhất của cuộc hội thoại (dùng cho AI context history)
    List<ChatMessage> findTop10ByConversationIdOrderByNgayTaoDesc(String conversationId);
}
