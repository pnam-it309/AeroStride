package com.example.be.repository;

import com.example.be.entity.TinNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, String> {

    @Modifying
    @Query("DELETE FROM TinNhan t WHERE t.cuocHoiThoai.id IN :conversationIds")
    void deleteByConversationIdIn(@Param("conversationIds") List<String> conversationIds);
}
