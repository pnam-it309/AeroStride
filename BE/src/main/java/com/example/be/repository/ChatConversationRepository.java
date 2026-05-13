package com.example.be.repository;

import com.example.be.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, String> {
    Optional<ChatConversation> findBySessionId(String sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM ChatConversation c WHERE c.type = 'INTERNAL' AND " +
           "((c.nhanVien.id = :id1 AND c.secondNhanVien.id = :id2) OR " +
           "(c.nhanVien.id = :id2 AND c.secondNhanVien.id = :id1))")
    Optional<ChatConversation> findInternalConversation(String id1, String id2);
}
