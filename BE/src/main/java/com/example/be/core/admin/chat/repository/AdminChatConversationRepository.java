package com.example.be.core.admin.chat.repository;

import com.example.be.entity.ChatConversation;
import com.example.be.repository.ChatConversationRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminChatConversationRepository extends ChatConversationRepository, JpaSpecificationExecutor<ChatConversation> {
    Optional<ChatConversation> findBySessionId(String sessionId);

    @Query("SELECT c FROM ChatConversation c WHERE c.type = 'INTERNAL' AND " +
           "((c.nhanVien.id = :id1 AND c.secondNhanVien.id = :id2) OR " +
           "(c.nhanVien.id = :id2 AND c.secondNhanVien.id = :id1))")
    Optional<ChatConversation> findInternalConversation(@Param("id1") String id1, @Param("id2") String id2);
}
