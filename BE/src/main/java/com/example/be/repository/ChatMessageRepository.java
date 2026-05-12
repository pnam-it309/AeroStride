package com.example.be.repository;

import com.example.be.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findByConversationIdOrderByNgayTaoAsc(String conversationId);
    List<ChatMessage> findByConversation_SessionIdOrderByNgayTaoAsc(String sessionId);
}
